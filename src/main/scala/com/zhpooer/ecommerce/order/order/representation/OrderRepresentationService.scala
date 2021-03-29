package com.zhpooer.ecommerce.order.order.representation

import cats.effect.Bracket
import cats.mtl.Raise
import cats.implicits._
import com.zhpooer.ecommerce.infrastructure.db.{JsonOperation, TransactionMrg}
import com.zhpooer.ecommerce.order.order.model.{Order, OrderItem}
import com.zhpooer.ecommerce.order.order.{OrderError, OrderRepository, OrderRepositoryAlg}
import doobie.util.transactor.Transactor
import io.circe.Encoder
import io.circe.generic.semiauto._

trait OrderRepresentationService[F[_]] {
  def getById(id: String)(implicit R: Raise[F, OrderError.OrderNotFound]): F[OrderRepr]
  def cqrsSync(orderId: String)(implicit R: Raise[F, OrderError.OrderNotFound]): F[Unit]
}

object OrderRepresentationService {
  def apply[F[_] : OrderRepresentationService]: OrderRepresentationService[F] = implicitly

  def impl[F[_]: Bracket[*[_], Throwable]: OrderRepositoryAlg: TransactionMrg]: OrderRepresentationService[F] = new OrderRepresentationService[F] with JsonOperation {
    override def getById(id: String)(implicit R: Raise[F, OrderError.OrderNotFound]): F[OrderRepr] = TransactionMrg[F].readOnly { implicit tx =>
      OrderRepository[F].getById(id).flatMap {
        case None => R.raise(OrderError.OrderNotFound(id))
        case Some(order) => orderToRepr(order).pure[F]
      }
    }

    override def cqrsSync(orderId: String)(implicit R: Raise[F, OrderError.OrderNotFound]): F[Unit] = {
      import doobie.implicits._

      implicit val orderSummaryEncoder: Encoder[OrderSummaryRepr] = {
        import io.circe.generic.auto._
        deriveEncoder
      }
      implicit val orderPut = jsonPut[OrderSummaryRepr]

      TransactionMrg[F].startTX { implicit txAsk =>
        for {
          maybeOrder <- OrderRepository[F].getById(orderId)
          orderSummary: OrderSummaryRepr <- maybeOrder match {
            case None => R.raise(OrderError.OrderNotFound(orderId))
            case Some(order) => orderToSummary(order).pure[F]
          }
          update = (fr"INSERT INTO ORDER_SUMMARY (ID, JSON_CONTENT) VALUES (${orderSummary.id}, ${orderSummary})" ++
            fr"ON DUPLICATE KEY UPDATE JSON_CONTENT=${orderSummary}").update
          tx <- txAsk.ask[Transactor[F]]
          _ <- update.run.transact(tx)
        } yield ()
      }
    }
  }

  def orderToSummary(order: Order): OrderSummaryRepr = order match {
    case Order(id, _, totalPrice, status, address, createdAt) =>
      OrderSummaryRepr(id, totalPrice, status, address, createdAt)
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
