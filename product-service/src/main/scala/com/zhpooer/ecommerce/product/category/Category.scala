package com.zhpooer.ecommerce.product.category

import cats.Monad
import cats.data.Chain
import cats.effect.Timer
import cats.implicits._
import cats.mtl.Tell
import com.zhpooer.ecommerce.infrastructure.Calendar

import java.time.Instant

case class Category(
  id: String,
  name: String,
  description: String,
  createdAt: Instant
)

object Category {
  def create[F[_]: Monad: Timer: DomainIdGen: Tell[*[_], Chain[CategoryEvent]]](
    name: String, description: String
  ): F[Category] = for {
    categoryId <- implicitly[DomainIdGen[F]].generateId
    now <- Calendar.now[F]
    c = Category(
      id = categoryId, name = name, description = description, createdAt = now
    )
    _ <- Tell.tellF[F](
      Chain(CategoryEvent.CategoryCreated(categoryId, name, description))
    )
  } yield c
}
