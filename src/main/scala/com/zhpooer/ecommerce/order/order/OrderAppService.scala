package com.zhpooer.ecommerce.order.order

import cats.data.Chain
import cats.effect.{Sync, Timer}
import com.zhpooer.ecommerce.order.order.OrderCommand.CreateOrderCommand
import com.zhpooer.ecommerce.order.order.model.{Order, OrderItem}
import cats.implicits._
import com.zhpooer.ecommerce.order.infrastructure.db.TransactionMrg

trait OrderAppService[F[_]] {
  def createOrder(createOrderCommand: CreateOrderCommand): F[Order]
}

object OrderAppService {
  def apply[F[_]: OrderAppService]: OrderAppService[F] = implicitly

  def impl[F[_]: Timer: Sync: OrderIdGenAlg: OrderEventDispatcher: TransactionMrg: OrderRepositoryAlg]: OrderAppService[F] = new OrderAppService[F] {
    override def createOrder(createOrderCommand: CreateOrderCommand): F[Order] =
      TransactionMrg[F].startTX { implicit askTX =>

        val orderItems = createOrderCommand.items.map {
          case OrderCommand.OrderItemCommand(productId, count, itemPrice) =>
            OrderItem(productId, count, itemPrice)
        }
        for {
          createdOrder <- Order.create(orderItems, createOrderCommand.address)
          _ <- OrderRepository[F].save(createdOrder)

          orderEvent <- OrderEvent(createdOrder.id, OrderCreated(
            createdOrder.totalPrice, createdOrder.address, createdOrder.items, createdOrder.createdAt
          ))
          _ <- OrderEventDispatcher[F].dispatch(Chain.one(orderEvent))
        } yield createdOrder
    }
  }
}
