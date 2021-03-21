package com.zhpooer.ecommerce.order.order.representation

import cats.Monad
import cats.mtl.Raise
import cats.implicits._
import com.zhpooer.ecommerce.infrastructure.db.TransactionMrg
import com.zhpooer.ecommerce.order.order.model.{Order, OrderItem}
import com.zhpooer.ecommerce.order.order.{OrderError, OrderRepository, OrderRepositoryAlg}

trait OrderRepresentationService[F[_]] {
  def getById(id: String)(implicit R: Raise[F, OrderError]): F[OrderRepr]
}

object OrderRepresentationService {
  def apply[F[_] : OrderRepresentationService]: OrderRepresentationService[F] = implicitly

  def impl[F[_]: Monad: OrderRepositoryAlg: TransactionMrg] = new OrderRepresentationService[F] {
    override def getById(id: String)(implicit R: Raise[F, OrderError]): F[OrderRepr] = TransactionMrg[F].readOnly { implicit tx =>
      OrderRepository[F].getById(id).flatMap {
        case None => R.raise(OrderError.OrderNotFound(id))
        case Some(order) => orderToRepr(order).pure[F]
      }
    }
  }

  def orderToRepr(order: Order): OrderRepr = order match {
    case Order(id, items, totalPrice, status, address, createdAt) =>
      OrderRepr(
        id = id,
        items = items.map(orderItemToRepr),
        totalPrice = totalPrice,
        status = status,
        address = address,
        createdAt = createdAt
      )
  }

  def orderItemToRepr(orderItem: OrderItem): OrderItemRepr = orderItem match {
    case OrderItem(productId, count, itemPrice) => OrderItemRepr(productId, count, itemPrice)
  }
}
