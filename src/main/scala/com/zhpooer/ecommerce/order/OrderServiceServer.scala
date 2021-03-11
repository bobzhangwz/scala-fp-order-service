package com.zhpooer.ecommerce.order

import cats.effect.{Blocker, ConcurrentEffect, ContextShift, Timer}
import cats.implicits._
import com.zhpooer.ecommerce.order.about.{AboutAlg, AboutRoutes}
import com.zhpooer.ecommerce.order.infrastructure.db.{DBManager, TransactionMrg}
import com.zhpooer.ecommerce.order.infrastructure.env._
import com.zhpooer.ecommerce.order.order.OrderRoutes
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

import scala.concurrent.ExecutionContext.global
import com.zhpooer.ecommerce.order.configuration.ConfigLoader
import com.zhpooer.ecommerce.order.configuration.AppConfig

object OrderServiceServer {

  def server[F[_]: ConcurrentEffect: ContextShift](implicit T: Timer[F]): Stream[F, Nothing] =
    Stream.eval(new ConfigLoader(sys.env).load[F]) >>= { appConfig =>
      serverStream(appConfig)
    }

  def serverStream[F[_]: ConcurrentEffect: ContextShift](appConfig: AppConfig)(implicit T: Timer[F]): Stream[F, Nothing] = {
    val blockAndTransactor = for {
      blocker <- Blocker[F]
      transactor <- DBManager.transactor[F](appConfig.dbConfig, blocker)
    } yield (blocker, transactor)

    Stream.resource(blockAndTransactor) >>= { case (_, transactor) =>
      implicit val transactionMrg = TransactionMrg.impl[F](transactor)

      for {
        client <- BlazeClientBuilder[F](global).stream
        implicit0(envAlg: EnvironmentAlg[F]) = Environment.impl[F]

        aboutAlg = AboutAlg.impl[F]
        orderAlg = order.alg.orderAlg[F]

        httpApp = (
          AboutRoutes.all[F](aboutAlg) <+>
            OrderRoutes.all[F](orderAlg)
        ).orNotFound

        finalHttpApp = Logger.httpApp(true, true)(httpApp)

        exitCode <- BlazeServerBuilder[F](global)
          .bindHttp(8080, "0.0.0.0")
          .withHttpApp(finalHttpApp)
          .serve
      } yield exitCode
    }
  }.drain
}
