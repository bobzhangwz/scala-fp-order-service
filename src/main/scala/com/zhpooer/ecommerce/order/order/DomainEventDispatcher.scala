package com.zhpooer.ecommerce.order.order

import cats.data.Chain
import cats.effect.{Sync, Timer}
import cats.implicits._
import cats.mtl.Tell
import software.amazon.awssdk.services.sns.model.PublishRequest
import io.circe.syntax._
import io.circe.generic.auto._
import software.amazon.awssdk.services.sns.SnsClient
import io.circe.Encoder

trait DomainEventDispatcher[F[_]] {
  def dispatch[T](events: Chain[DomainEvent[T]])(implicit E: Encoder[T]): F[Unit]
  def tell[S](orderId: String, detail: S)(implicit T: Tell[F, Chain[DomainEvent[S]]]): F[Unit]
}

object DomainEventDispatcher {

  def apply[F[_]: DomainEventDispatcher]: DomainEventDispatcher[F] = implicitly

  def impl[F[_]: Sync: Timer](snsClient: SnsClient, orderEventPublishQueueArn: String): DomainEventDispatcher[F] = new DomainEventDispatcher[F] {

    def dispatch[T](events: Chain[DomainEvent[T]])(implicit E: Encoder[T]): F[Unit] = {
      events.traverse_(e => {
        val request = PublishRequest.builder()
          .message(e.asJson.noSpaces)
          .targetArn(orderEventPublishQueueArn)
          .build()

        Sync[F].delay({snsClient.publish(request)})
      })
    }

    def tell[S](
      id: String, detail: S)(implicit T: Tell[F, Chain[DomainEvent[S]]]): F[Unit] = {
      for {
        event: DomainEvent[S] <- DomainEvent[F, S](id, detail)
        _ <- Tell.tellF[F](Chain.one(event))
      } yield ()
    }
  }
}
