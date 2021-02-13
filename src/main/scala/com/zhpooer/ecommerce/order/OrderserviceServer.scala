package com.zhpooer.ecommerce.order

import cats.effect.{ConcurrentEffect, Timer}
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

object OrderserviceServer {

  def stream[F[_]: ConcurrentEffect](implicit T: Timer[F]): Stream[F, Nothing] = {
    for {
      client <- BlazeClientBuilder[F](global).stream
      helloWorldAlg = HelloWorld.impl[F]
      jokeAlg = Jokes.impl[F](client)

      implicit0(envAlg: EnvironmentAlg[F]) = Environment.impl[F]
      aboutAlg = AboutAlg.impl[F]

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      httpApp = (
        OrderserviceRoutes.helloWorldRoutes[F](helloWorldAlg) <+>
        OrderserviceRoutes.jokeRoutes[F](jokeAlg) <+>
        AboutRoutes.all[F](aboutAlg)
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      exitCode <- BlazeServerBuilder[F](global)
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
  }.drain
}
