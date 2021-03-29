package com.zhpooer.ecommerce.infrastructure

import cats.Monad
import cats.data.Chain
import cats.effect.Timer
import cats.implicits._
import cats.mtl.Tell

package object event {
  def tell[F[_]: UUIDFactory: Monad: Timer, A](eventId: String, detail: A)(implicit T: Tell[F, Chain[DomainEvent[A]]]): F[Unit] = {
    for {
      event: DomainEvent[A] <- DomainEvent[F, A](eventId, detail)
      _ <- Tell.tellF[F](Chain.one(event))
    } yield ()
  }
}
