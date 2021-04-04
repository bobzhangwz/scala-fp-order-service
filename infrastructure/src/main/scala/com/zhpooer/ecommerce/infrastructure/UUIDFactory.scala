package com.zhpooer.ecommerce.infrastructure

import cats.syntax.all._
import cats.{Applicative, Defer}

import java.util.UUID

trait UUIDFactory[F[_]] {
  def create: F[UUID]
}

object UUIDFactory {

  def apply[F[_]: UUIDFactory]: UUIDFactory[F] = implicitly

  def impl[F[_]: Defer: Applicative] = new UUIDFactory[F] {
    override def create: F[UUID] = Defer[F].defer(UUID.randomUUID().pure)
  }
}
