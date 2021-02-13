package com.zhpooer.ecommerce.order.order

import com.zhpooer.ecommerce.order.order.model.Address
import com.zhpooer.ecommerce.order.order.model.OrderItem
import java.time.Instant
import cats.effect.Timer
import cats.implicits._
import cats.effect.Sync
import java.util.UUID
import java.util.concurrent.TimeUnit

case class OrderEvent private (
  eventId: String, orderId: String,
  detail: OrderEventDetail, eventCreatedAt: Instant
)

object OrderEvent {
  def apply[F[_]: Sync: Timer](
    orderId: String, detail: OrderEventDetail): F[OrderEvent] = {
    for {
      eventId <- Sync[F].delay(UUID.randomUUID().toString())
      now <- Timer[F].clock.monotonic(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli(_))
    } yield OrderEvent(eventId, orderId, detail, now)
  }
}

sealed trait OrderEventDetail

case class OrderAddressChanged(
  oldAddress: String,
  newAddress: String
) extends OrderEventDetail

case class OrderCreated(
  totalPrice: BigDecimal,
  address: Address,
  items: List[OrderItem],
  createdAt: Instant
) extends OrderEventDetail

case object OrderPaid extends OrderEventDetail

case class OrderProductChanged(
  productId: String, originCount: Int, newCount: Int
) extends OrderEventDetail

