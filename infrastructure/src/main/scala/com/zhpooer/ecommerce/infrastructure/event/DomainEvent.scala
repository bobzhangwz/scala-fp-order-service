package com.zhpooer.ecommerce.infrastructure.event

import cats.effect.{Sync, Timer}
import cats.implicits._
import io.circe.{Decoder, Encoder}

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.reflect.ClassTag
import io.circe.generic.semiauto._

case class DomainEvent[T] private (
  eventId: String, subjectId: String,
  detail: T, createdAt: Instant
)

object DomainEvent {
  def apply[F[_]: Sync: Timer, T](
    orderId: String, detail: T): F[DomainEvent[T]] = {
    for {
      eventId <- Sync[F].delay(UUID.randomUUID().toString)
      now <- Timer[F].clock.monotonic(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli)
    } yield DomainEvent(eventId, orderId, detail, now)
  }

  implicit def domainEventEncoder[T: Encoder: ClassTag]: Encoder[DomainEvent[T]] = {
    Encoder.forProduct5("eventId", "subjectId", "createdAt", "type", "detail") { e =>
      val eventType = implicitly[ClassTag[T]].runtimeClass.getSimpleName
      (e.eventId, e.subjectId, e.createdAt, eventType, e.detail)
    }
  }

  implicit def domainEventDecoder[T: Decoder]: Decoder[DomainEvent[T]] = deriveDecoder
}
