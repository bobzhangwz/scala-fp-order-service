package com.zhpooer.ecommerce.product.category

import cats.Monad
import cats.effect.Timer

import java.time.Instant

case class Category(
  id: String,
  name: String,
  description: String,
  createdAt: Instant
)

object Category {
  def create[F[_]: Monad: Timer: CategoryIdGen](name: String, description: String): F[Category] = ???
}

trait CategoryIdGen[F[_]] {
  def genId: F[String]
}