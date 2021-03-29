package com.zhpooer.ecommerce.infrastructure.event


import cats.data.Chain
import cats.effect.{Sync, Timer}
import cats.implicits._
import software.amazon.awssdk.services.sns.model.PublishRequest
import io.circe.syntax._
import software.amazon.awssdk.services.sns.SnsClient
import io.circe.Encoder

import scala.reflect.ClassTag

trait DomainEventDispatcher[F[_], A] {
  def dispatch(events: Chain[DomainEvent[A]]): F[Unit]
}

object DomainEventDispatcher {

  class PartialDomainEventDispatcher[F[_]] {
    def apply[A](implicit d: DomainEventDispatcher[F, A]): DomainEventDispatcher[F, A] = d
  }

  def impl[F[_]: Sync: Timer, A: Encoder: ClassTag](snsClient: SnsClient, eventPublishQueueArn: String): DomainEventDispatcher[F, A] = new DomainEventDispatcher[F, A] {
    implicit val domainEventEncoder = DomainEvent.domainEventEncoder[A]
    def dispatch(events: Chain[DomainEvent[A]]): F[Unit] = {
      events.traverse_(e => {
        val request = PublishRequest.builder()
          .message(e.asJson.noSpaces)
          .messageDeduplicationId(e.globalIdentifier)
          .targetArn(eventPublishQueueArn)
          .build()

        Sync[F].delay({snsClient.publish(request)})
      })
    }
  }
}
