package com.zhpooer.ecommerce.order.order.model

import cats.Monad
import cats.data.Chain
import cats.effect.Timer
import com.zhpooer.ecommerce.order.order._
import cats.implicits._
import cats.mtl.{Ask, Raise, Tell}
import com.zhpooer.ecommerce.infrastructure.Calendar

import java.time.Instant

case class Order(
  id: String,
  items: List[OrderItem],
  totalPrice: BigDecimal,
  status: OrderStatus,
  address: Address,
  createdAt: Instant
)

sealed trait OrderStatus

object OrderStatus {
  case object Created extends OrderStatus
  case object Paid extends OrderStatus
}

case class Address(
  province: String,
  city: String,
  detail: String
)

object Order {

  def create[F[_]: Timer: OrderIdGenAlg: Monad: Tell[*[_], Chain[OrderEvent]]](
    items: List[OrderItem], address: Address): F[Order] = {
    val totalPrice = items.map(i => i.count * i.itemPrice).sum
    for {
      orderId <- OrderIdGen[F].gen
      now <- Calendar.now[F]
      o = Order(
        id = orderId, items = items,
        totalPrice = totalPrice, status = OrderStatus.Created,
        address = address, createdAt = now
      )
      _ <- Tell.tellF[F](
        Chain.one(OrderCreated(orderId, o.totalPrice, o.address, o.items, o.createdAt))
      )
    } yield o
  }

  def changeProductCount[F[_]: Monad](productId: String, count: Int)(
    implicit A: Ask[F, Order], R: Raise[F, OrderError]
  ): F[Order] = {
    for {
      originOrder <- A.ask[Order]
      _ <- R.raise(OrderError.OrderCannotBeModified(originOrder.id)).whenA(originOrder.status == OrderStatus.Paid)
      productItemExists = originOrder.items.exists(_.productId == productId)
      _ <- R.raise(OrderError.ProductNotInOrder(productId, originOrder.id)).whenA(!productItemExists)
      updatedItems = originOrder.items.map {
        case p if p.productId == productId => p.copy(count = count)
        case p => p
      }
      totalPrice = updatedItems.map(i => i.count * i.itemPrice).sum
    } yield originOrder.copy(items = updatedItems, totalPrice = totalPrice)
  }

  def pay[F[_]: Monad](paidPrice: BigDecimal)(
    implicit A: Ask[F, Order], R: Raise[F, OrderError]
  ): F[Order] = {
    for {
      originOrder <- A.ask[Order]
      _ <- R.raise(OrderError.PaidPriceNotSameWithOrderPrice(originOrder.id)).whenA(
        originOrder.totalPrice != paidPrice
      )
    } yield originOrder.copy(status = OrderStatus.Paid)
  }

  def changeAddressDetail[F[_]: Monad](detail: String)(
    implicit A: Ask[F, Order], R: Raise[F, OrderError]
  ): F[Order] = {
    for {
      originOrder <- A.ask[Order]
      _ <- R.raise(OrderError.OrderCannotBeModified(originOrder.id)).whenA(originOrder.status == OrderStatus.Paid)
      updatedAddress = originOrder.address.copy(detail = detail)
    } yield originOrder.copy(address = updatedAddress)
  }
}
