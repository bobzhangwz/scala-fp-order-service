package com.zhpooer.ecommerce.order.order

import cats.{Functor, Monad}
import cats.data.Chain
import cats.effect.Timer
import cats.implicits._
import cats.mtl.{Ask, Raise, Tell}
import com.zhpooer.ecommerce.infrastructure.db.TransactionMrg
import com.zhpooer.ecommerce.order.order.OrderCommand.{ChangeProductCountCommand, CreateOrderCommand, PayOrderCommand}
import com.zhpooer.ecommerce.order.order.OrderError.OrderNotFound
import com.zhpooer.ecommerce.order.order.model.{Order, OrderItem}

trait OrderAppService[F[_]] {
  def createOrder(createOrderCommand: CreateOrderCommand): F[Order]

  def changeProductCount(orderId: String, command: ChangeProductCountCommand)(
    implicit R: Raise[F, OrderError]
  ): F[Unit]

  def pay(orderId: String, command: PayOrderCommand)(
    implicit R: Raise[F, OrderError]
  ): F[Unit]

  def changeAddressDetail(orderId: String, detail: String)(
    implicit R: Raise[F, OrderError]
  ): F[Unit]
}

object OrderAppService {
  def apply[F[_]: OrderAppService]: OrderAppService[F] = implicitly

  def impl[
    F[_]: Timer: Monad: OrderIdGenAlg : OrderEventDispatcher: TransactionMrg: OrderRepositoryAlg
  ]: OrderAppService[F] = new OrderAppService[F] {

    implicit val tellInstance = new Tell[F, Chain[OrderDomainEvent]] {
      override def functor: Functor[F] = implicitly
      override def tell(l: Chain[OrderDomainEvent]): F[Unit] = implicitly[OrderEventDispatcher[F]].dispatch(l)
    }

    override def createOrder(createOrderCommand: CreateOrderCommand): F[Order] =
      TransactionMrg[F].startTX { implicit askTX =>

        val orderItems = createOrderCommand.items.map {
          case OrderCommand.OrderItemCommand(productId, count, itemPrice) =>
            OrderItem(productId, count, itemPrice)
        }
        for {
          createdOrder <- Order.create(orderItems, createOrderCommand.address)
          _ <- OrderRepository[F].save(createdOrder)
        } yield createdOrder
    }

    override def changeProductCount(orderId: String, command: ChangeProductCountCommand)(
      implicit R: Raise[F, OrderError]
    ): F[Unit] =
      TransactionMrg[F].startTX { implicit askTX =>
        for {
          maybeOrder <- OrderRepository[F].getById(orderId)
          _ <- R.raise(OrderNotFound(orderId)).whenA(maybeOrder.isEmpty)
          implicit0(order: Ask[F, Order]) = Ask.const[F, Order](maybeOrder.get)
          _ <- Order.changeProductCount[F](command.productId, command.count) >>= OrderRepository[F].save
        } yield ()
      }

    override def pay(orderId: String, command: PayOrderCommand)(
      implicit R: Raise[F, OrderError]
    ): F[Unit] =
      TransactionMrg[F].startTX { implicit askTX =>
        for {
          maybeOrder <- OrderRepository[F].getById(orderId)
          _ <- R.raise(OrderNotFound(orderId)).whenA(maybeOrder.isEmpty)
          implicit0(order: Ask[F, Order]) = Ask.const[F, Order](maybeOrder.get)
          _ <- Order.pay[F](command.paidPrice) >>= OrderRepository[F].save
        } yield ()
      }

    override def changeAddressDetail(
      orderId: String, detail: String)(implicit R: Raise[F, OrderError]): F[Unit] = TransactionMrg[F].startTX { implicit askTX =>
      for {
        maybeOrder <- OrderRepository[F].getById(orderId)
        _ <- R.raise(OrderNotFound(orderId)).whenA(maybeOrder.isEmpty)
        implicit0(order: Ask[F, Order]) = Ask.const[F, Order](maybeOrder.get)
        _ <- Order.changeAddressDetail[F](detail) >>= OrderRepository[F].save
      } yield ()
    }
  }
}
