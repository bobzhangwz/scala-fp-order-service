package com.zhpooer.ecommerce.infrastructure.event


import cats.data.{Chain, WriterT}
import cats.effect.{Sync, Timer}
import cats.implicits._
import com.zhpooer.ecommerce.infrastructure.UUIDFactory
import software.amazon.awssdk.services.sns.model.PublishRequest
import io.circe.syntax._
import software.amazon.awssdk.services.sns.SnsClient
import io.circe.Encoder

import scala.reflect.ClassTag

trait EventDispatcher[F[_], A] {
  def dispatch(events: Chain[A]): F[Unit]
  def dispatchAndExtract[B](eventsWriter: WriterT[F, Chain[A], B]): F[B]
}

object EventDispatcher {

  class PartialDomainEventDispatcher[F[_]] {
    def apply[A](implicit d: EventDispatcher[F, A]): EventDispatcher[F, A] = d
  }

  def impl[F[_]: UUIDFactory: Sync: Timer, A: Encoder: ClassTag](snsClient: SnsClient, eventPublishQueueArn: String): EventDispatcher[F, A] = new EventDispatcher[F, A] {
    implicit val domainEventEncoder = DomainEvent.domainEventEncoder[A]
    def dispatch(events: Chain[A]): F[Unit] = {

      events.traverse_(e => {
        for {
          domainEvent <- DomainEvent(e)
          request = PublishRequest.builder()
            .message(domainEvent.asJson.noSpaces)
            .messageDeduplicationId(domainEvent.eventId)
            .targetArn(eventPublishQueueArn)
            .build()
          _ <- Sync[F].delay{snsClient.publish(request)}
        } yield ()
      })
    }

    def dispatchAndExtract[B](eventsWriter: WriterT[F, Chain[A], B]): F[B] = {
      for {
        (events, b) <- eventsWriter.run
        _ <- dispatch(events)
      } yield b
    }
  }
}
