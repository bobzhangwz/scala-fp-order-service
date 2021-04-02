package com.zhpooer.ecommerce.product.product

import cats.Monad
import cats.data.{Chain, Kleisli, ReaderT}
import cats.effect.Timer
import cats.mtl.Tell

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

  def updateName[F[_]: Monad: Timer : Tell[*[_], Chain[ProductEvent]]](newName: String): ReaderT[F, Product, Product] =
    for {
      p <- ReaderT.ask[F, Product]
      _ <- Kleisli.liftF(Tell.tellF[F](
        Chain(ProductEvent.ProductNameUpdatedEvent(p.id, oldName = p.name, newName = newName))
      ))
    } yield p.copy(name = newName)
}
