package com.zhpooer.ecommerce.order

import cats.effect.{Blocker, ConcurrentEffect, ContextShift, Timer}
import cats.implicits._
import com.zhpooer.ecommerce.order.about.{AboutAlg, AboutRoutes}
import com.zhpooer.ecommerce.infrastructure.db.{DBManager, TransactionMrg}
import com.zhpooer.ecommerce.infrastructure.env._
import com.zhpooer.ecommerce.order.order.OrderRoutes
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import scala.concurrent.ExecutionContext.global
import com.zhpooer.ecommerce.order.configuration.ConfigLoader
import com.zhpooer.ecommerce.order.configuration.AppEnv

object OrderServiceServer {

  def serve[F[_]: ConcurrentEffect: ContextShift](implicit T: Timer[F]): Stream[F, Nothing] =
    Stream.eval(new ConfigLoader(sys.env).load[F]) >>= { appEnv =>
      serverStream(appEnv)
    }

  private def serverStream[F[_]: ConcurrentEffect: ContextShift](appEnv: AppEnv)(implicit T: Timer[F]): Stream[F, Nothing] = {
    val blockAndTransactor = for {
      blocker <- Blocker[F]
      transactor <- DBManager.transactor[F](appEnv.dbConfig, blocker)
    } yield (blocker, transactor)

    Stream.resource(blockAndTransactor) >>= { case (_, transactor) =>
      implicit val transactionMrg = TransactionMrg.impl[F](transactor)

      for {
        _ <- BlazeClientBuilder[F](global).stream

        implicit0(envAlg: EnvironmentAlg[F]) = Environment.impl[F]

        aboutAlg = AboutAlg.impl[F]
        orderAlg = order.alg[F](appEnv.snsClient, appEnv.orderEventPublisherArn)

        httpApp = (
          AboutRoutes.all[F](aboutAlg) <+>
            OrderRoutes.all[F](orderAlg.orderAppSvc, orderAlg.orderReprSvc)
        ).orNotFound

        finalHttpApp = Logger.httpApp(true, true)(httpApp)
        apiConfig = appEnv.apiConfig

        exitCode <- BlazeServerBuilder[F](global)
          .bindHttp(apiConfig.port, apiConfig.endpoint)
          .withHttpApp(finalHttpApp)
          .serve
      } yield exitCode
    }
  }.drain
}
