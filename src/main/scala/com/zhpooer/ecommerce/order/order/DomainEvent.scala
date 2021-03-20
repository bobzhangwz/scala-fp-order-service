package com.zhpooer.ecommerce.order.order

import cats.effect.{Sync, Timer}
import cats.implicits._
import com.zhpooer.ecommerce.order.order.model.{Address, OrderItem}

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import io.circe.Encoder
import io.circe.generic.semiauto._
import io.circe.generic.auto._
import scala.reflect.ClassTag

case class DomainEvent[T] private (
  eventId: String, subjectId: String,
  detail: T, createdAt: Instant
)

object DomainEvent {
  def apply[F[_]: Sync: Timer, T](
    orderId: String, detail: T): F[DomainEvent[T]] = {
    for {
      eventId <- Sync[F].delay(UUID.randomUUID().toString())
      now <- Timer[F].clock.monotonic(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli(_))
    } yield DomainEvent(eventId, orderId, detail, now)
  }

  implicit def domainEventEncoder[T: Encoder: ClassTag]: Encoder[DomainEvent[T]] =
    Encoder.forProduct5("eventId", "subjectId", "createdAt", "type", "detail") { e =>
      val eventType = implicitly[ClassTag[T]].runtimeClass.getSimpleName()
      (e.eventId, e.subjectId, e.createdAt, eventType, e.detail)
    }
}

sealed trait OrderEventDetail

object OrderEventDetail {
  implicit val orderEventDecoder: Encoder[OrderEventDetail] =
    deriveEncoder
}

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
