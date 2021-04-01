package com.zhpooer.ecommerce.infrastructure.event

import cats.Monad
import cats.effect.Timer
import cats.implicits._
import com.zhpooer.ecommerce.infrastructure.{Calendar, UUIDFactory}
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

import java.time.Instant
import scala.reflect.ClassTag

case class DomainEvent[T] private (
  globalIdentifier: String, eventId: String,
  detail: T, createdAt: Instant
)

object DomainEvent {
  def apply[F[_]: UUIDFactory: Monad: Timer, T](
    eventId: String, detail: T): F[DomainEvent[T]] = {
    for {
      gid <- UUIDFactory[F].create
      now <- Calendar.now[F]
    } yield DomainEvent(gid.toString, eventId, detail, now)
  }

  def domainEventEncoder[T: Encoder: ClassTag]: Encoder[DomainEvent[T]] = {
    Encoder.forProduct5("globalIdentifier", "eventId", "createdAt", "category", "detail") { e =>
      val eventType = implicitly[ClassTag[T]].runtimeClass.getSimpleName
      (e.globalIdentifier, e.eventId, e.createdAt, eventType, e.detail)
    }
  }

  implicit def domainEventDecoder[T: Decoder]: Decoder[DomainEvent[T]] = deriveDecoder
}
