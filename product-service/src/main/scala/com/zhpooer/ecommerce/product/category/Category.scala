package com.zhpooer.ecommerce.product.category

import cats.Monad
import cats.data.Chain
import cats.effect.{Sync, Timer}
import cats.implicits._
import cats.mtl.Tell
import com.zhpooer.ecommerce.infrastructure.{Calendar, UUIDFactory, event}

import java.time.Instant
import java.util.UUID

case class Category(
  id: String,
  name: String,
  description: String,
  createdAt: Instant
)

object Category {
  def create[F[_]: Monad: Timer: CategoryIdGen: UUIDFactory: Tell[*[_], Chain[CategoryDomainEvent]]](
    name: String, description: String
  ): F[Category] = for {
    categoryId <- CategoryIdGen[F].genId
    now <- Calendar.now[F]
    c = Category(
      id = categoryId, name = name, description = description, createdAt = now
    )
    _ <- event.tell[F, CategoryEvent](
      categoryId, CategoryEvent.CategoryCreated(name, description)
    )
  } yield c
}

trait CategoryIdGen[F[_]] {
  def genId: F[String]
}

object CategoryIdGen {
  def apply[F[_]: CategoryIdGen]: CategoryIdGen[F] = implicitly

  def impl[F[_]: Sync] = new CategoryIdGen[F] {
    override def genId: F[String] = Sync[F].delay(UUID.randomUUID().toString)
  }
}