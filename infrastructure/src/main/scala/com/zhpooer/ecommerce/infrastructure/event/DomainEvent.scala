package com.zhpooer.ecommerce.infrastructure.event

import cats.Monad
import cats.effect.Timer
import cats.implicits._
import com.zhpooer.ecommerce.infrastructure.{Calendar, UUIDFactory}
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

import java.time.Instant
import scala.reflect.ClassTag

case class DomainEvent[T] private (
  eventId: String, detail: T, createdAt: Instant
)

object DomainEvent {
  def apply[F[_]: UUIDFactory: Monad: Timer, T](detail: T): F[DomainEvent[T]] = {
    for {
      gid <- UUIDFactory[F].create
      now <- Calendar.now[F]
    } yield DomainEvent(gid.toString, detail, now)
  }

  def domainEventEncoder[T: Encoder: ClassTag]: Encoder[DomainEvent[T]] = {
    deriveEncoder[DomainEvent[T]].mapJson(_.deepMerge(
      ("type" -> implicitly[ClassTag[T]].runtimeClass.getSimpleName).asJson
    ))
  }

  implicit def domainEventDecoder[T: Decoder]: Decoder[DomainEvent[T]] = deriveDecoder
}
