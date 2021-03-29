package com.zhpooer.ecommerce.order

import cats.effect.{Blocker, ConcurrentEffect, ContextShift, ExitCode, Timer}
import cats.implicits._
import com.zhpooer.ecommerce.infrastructure.ErrorHandler
import com.zhpooer.ecommerce.order.about.{AboutAlg, AboutRoutes}
import com.zhpooer.ecommerce.infrastructure.db.{DBManager, TransactionMrg}
import com.zhpooer.ecommerce.infrastructure.env._
import com.zhpooer.ecommerce.order.order.{OrderDomainEvent, OrderError, OrderRoutes, SQSEventSubscriber}
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

import scala.concurrent.ExecutionContext.global
import com.zhpooer.ecommerce.order.configuration.{ApiConfig, AppEnv, ConfigLoader}
import org.http4s.HttpApp

object OrderServiceServer {

  def serve[F[_]: ConcurrentEffect: ContextShift](implicit T: Timer[F]): Stream[F, Nothing] =
    Stream.eval(new ConfigLoader(sys.env).load[F]) >>= { appEnv =>
      orderServiceStream(appEnv)
    }

  private def startHttpServer[F[_]: ConcurrentEffect: ContextShift: Timer](
    httpApp: HttpApp[F], apiConfig: ApiConfig
  ): Stream[F, ExitCode] =
    for {
      _ <- BlazeClientBuilder[F](global).stream
      finalHttpApp = Logger.httpApp(true, true)(httpApp)
      exitCode <- BlazeServerBuilder[F](global)
        .bindHttp(apiConfig.port, apiConfig.endpoint)
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode

  private def orderServiceStream[F[_]: ConcurrentEffect: ContextShift](appEnv: AppEnv)(implicit T: Timer[F]): Stream[F, Nothing] = {
    val blockAndTransactor = for {
      blocker <- Blocker[F]
      transactor <- DBManager.transactor[F](appEnv.dbConfig, blocker)
    } yield (blocker, transactor)

    Stream.resource(blockAndTransactor) >>= { case (blocker, transactor) =>
      implicit val transactionMrg = TransactionMrg.impl[F](transactor)
      implicit val envAlg: EnvironmentAlg[F] = Environment.impl[F]

      val aboutAlg = AboutAlg.impl[F]
      val orderAlg = order.alg[F](appEnv.snsClient, appEnv.orderEventPublisherArn)

      val orderEventSubscriber =
        new SQSEventSubscriber[F, OrderDomainEvent](appEnv.sqsClient, appEnv.orderEventListenerUrl, blocker)
      val orderEventProcessor = orderEventSubscriber.eventStream.evalMap(e =>
        orderAlg.orderReprSvc.cqrsSync(e.subjectId)(ErrorHandler.raiseAny[F, OrderError])
      ).handleErrorWith(_ => Stream.emit(())).foreverM

      val httpApp = (
        AboutRoutes.all[F](aboutAlg) <+>
          OrderRoutes.all[F](orderAlg.orderAppSvc, orderAlg.orderReprSvc)
        ).orNotFound

      startHttpServer[F](httpApp, appEnv.apiConfig) concurrently orderEventProcessor.drain

    }
  }.drain
}
