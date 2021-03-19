package com.zhpooer.ecommerce.order.order

import cats.data.Chain
import cats.effect.{Sync, Timer}
import cats.implicits._
import cats.mtl.Tell
import software.amazon.awssdk.services.sns.model.PublishRequest
import io.circe.syntax._
import io.circe.generic.auto._
import software.amazon.awssdk.services.sns.SnsClient

trait OrderEventDispatcher[F[_]] {
  def dispatch(events: Chain[OrderEvent]): F[Unit]
  def tell(orderId: String, detail: OrderEventDetail)(implicit T: Tell[F, Chain[OrderEvent]]): F[Unit]
}

object OrderEventDispatcher {

  def apply[F[_]: OrderEventDispatcher]: OrderEventDispatcher[F] = implicitly

  def impl[F[_]: Sync: Timer](snsClient: SnsClient, orderEventPublishQueueArn: String): OrderEventDispatcher[F] = new OrderEventDispatcher[F] {

    override def dispatch(events: Chain[OrderEvent]): F[Unit] = {
      events.traverse_(e => {
        val request = PublishRequest.builder()
          .message(e.asJson.noSpaces)
          .targetArn(orderEventPublishQueueArn)
          .build()

        Sync[F].delay({snsClient.publish(request)})
      })
    }

    override def tell(
      orderId: String, detail: OrderEventDetail)(implicit T: Tell[F, Chain[OrderEvent]]): F[Unit] = {
      for {
        orderEvent <- OrderEvent(orderId, detail)
        _ <- Tell.tellF[F](Chain.one(orderEvent))
      } yield ()
    }
  }
}
