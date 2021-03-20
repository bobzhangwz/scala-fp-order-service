package com.zhpooer.ecommerce.infrastructure.event


import cats.data.Chain
import cats.effect.{Sync, Timer}
import cats.implicits._
import cats.mtl.Tell
import software.amazon.awssdk.services.sns.model.PublishRequest
import io.circe.syntax._
import io.circe.generic.auto._
import software.amazon.awssdk.services.sns.SnsClient
import io.circe.Encoder

trait DomainEventDispatcher[F[_], A] {
  def dispatch(events: Chain[DomainEvent[A]]): F[Unit]
  def tell(orderId: String, detail: A)(implicit T: Tell[F, Chain[DomainEvent[A]]]): F[Unit]
}

object DomainEventDispatcher {

  class PartialDomainEventDispatcher[F[_]] {
    def apply[A](implicit d: DomainEventDispatcher[F, A]): DomainEventDispatcher[F, A] = d
  }

  def impl[F[_]: Sync: Timer, A: Encoder](snsClient: SnsClient, eventPublishQueueArn: String): DomainEventDispatcher[F, A] = new DomainEventDispatcher[F, A] {

    def dispatch(events: Chain[DomainEvent[A]]): F[Unit] = {
      events.traverse_(e => {
        val request = PublishRequest.builder()
          .message(e.asJson.noSpaces)
          .messageDeduplicationId(e.eventId)
          .targetArn(eventPublishQueueArn)
          .build()

        Sync[F].delay({snsClient.publish(request)})
      })
    }

    def tell(id: String, detail: A)(implicit T: Tell[F, Chain[DomainEvent[A]]]): F[Unit] = {
      for {
        event: DomainEvent[A] <- DomainEvent[F, A](id, detail)
        _ <- Tell.tellF[F](Chain.one(event))
      } yield ()
    }
  }
}
