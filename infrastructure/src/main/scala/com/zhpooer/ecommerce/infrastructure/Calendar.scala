package com.zhpooer.ecommerce.infrastructure

import cats.Functor
import cats.effect.Timer

import java.time.Instant
import java.util.concurrent.TimeUnit
import cats.syntax.all._

object Calendar {
  def now[F[_]: Timer: Functor]: F[Instant] = Timer[F].clock.monotonic(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli)
}
