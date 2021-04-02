package com.zhpooer.ecommerce.product.product

import cats.Monad
import cats.data.{Chain, Kleisli, ReaderT}
import cats.effect.Timer
import cats.mtl.Tell
import com.zhpooer.ecommerce.infrastructure.{Calendar, IdGenerator}
import cats.implicits._

import java.time.Instant

case class Product(
  id: String,
  name: String,
  description: String,
  price: BigDecimal,
  createdAt: Instant,
  inventory: Int,
  categoryId: String
)

object Product {

  def create[F[_]: IdGenerator[*[_], String]: Monad: Timer: Tell[*[_], Chain[ProductEvent]]](
    name: String, description: String, price: BigDecimal, categoryId: String): F[Product] = {
    for {
      productId <- IdGenerator[F, String].generateId
      now <- Calendar.now
      p = Product(
        id = productId,
        name = name,
        description = description,
        price = price,
        createdAt = now,
        inventory = 0,
        categoryId = categoryId
      )
      e = ProductEvent.ProductCreatedEvent(
        productId = productId, name = name, description = description, price = price, createdAt = now
      )
      _ <- Tell.tellF[F](Chain(e))
    } yield p
  }

  def updateName[F[_]: Monad : Tell[*[_], Chain[ProductEvent]]](newName: String): ReaderT[F, Product, Product] =
    for {
      p <- ReaderT.ask[F, Product]
      _ <- Kleisli.liftF(Tell.tellF[F](
        Chain(ProductEvent.ProductNameUpdatedEvent(p.id, oldName = p.name, newName = newName))
      ))
    } yield p.copy(name = newName)

}
