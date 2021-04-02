package com.zhpooer.ecommerce.product.product

import cats.Monad
import cats.data.{Kleisli, ReaderT}
import cats.effect.Timer
import cats.mtl.Tell
import com.zhpooer.ecommerce.infrastructure.{UUIDFactory, event}

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
  def updateName[F[_]: Monad: Timer: UUIDFactory : Tell[*[_], ProductDomainEvents]](newName: String): ReaderT[F, Product, Product] =
    for {
      p <- ReaderT.ask[F, Product]
      _ <- Kleisli.liftF(event.tell[F, ProductEvent](
        p.id, ProductEvent.ProductNameUpdatedEvent(p.id, oldName = p.name, newName = newName)
      ))
    } yield p.copy(name = newName)
}
