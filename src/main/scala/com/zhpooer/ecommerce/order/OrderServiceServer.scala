package com.zhpooer.ecommerce.order

import cats.effect.{Blocker, ConcurrentEffect, ContextShift, Timer}
import cats.implicits._
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger

import scala.concurrent.ExecutionContext.global
import com.zhpooer.ecommerce.order.infrastructure.env._
import com.zhpooer.ecommerce.order.about.AboutAlg
import com.zhpooer.ecommerce.order.about.AboutRoutes
import com.zhpooer.ecommerce.order.infrastructure.db.{DBManager, TransactionMrg}
import com.zhpooer.ecommerce.order.order.OrderRoutes

object OrderServiceServer {

  def stream[F[_]: ConcurrentEffect: ContextShift](implicit T: Timer[F]): Stream[F, Nothing] = {
    Stream.resource(Blocker[F]) >>= { blocker =>

      val txResource = DBManager.transactor[F](blocker)
      implicit val transactionMrg = TransactionMrg.impl[F](txResource)

      import order.implicits._

      for {
        client <- BlazeClientBuilder[F](global).stream

        implicit0(envAlg: EnvironmentAlg[F]) = Environment.impl[F]
        aboutAlg = AboutAlg.impl[F]

        httpApp = (
            AboutRoutes.all[F](aboutAlg) <+>
            OrderRoutes.all[F]
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
