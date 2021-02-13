package com.zhpooer.ecommerce.order.order.model

import cats.Functor
import cats.mtl.Ask
import cats.implicits._

case class OrderItem(
  productId: String,
  count: Int,
  itemPrice: BigDecimal
)

object OrderItem {
  def totalPrice[F[_]: Functor](implicit A: Ask[F, OrderItem]): F[BigDecimal] =
    A.ask[OrderItem].map(i => i.itemPrice * i.count)
}
