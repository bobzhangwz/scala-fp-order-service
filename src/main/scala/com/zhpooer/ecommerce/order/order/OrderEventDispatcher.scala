package com.zhpooer.ecommerce.order.order

import cats.data.Chain
import cats.effect.{Sync, Timer}
import cats.implicits._
import cats.mtl.Tell

trait OrderEventDispatcher[F[_]] {
  def dispatch(events: Chain[OrderEvent]): F[Unit]
  def tell(orderId: String, detail: OrderEventDetail)(implicit T: Tell[F, Chain[OrderEvent]]): F[Unit]
}

object OrderEventDispatcher {
  def apply[F[_]: OrderEventDispatcher]: OrderEventDispatcher[F] = implicitly

  def impl[F[_]: Sync: Timer]: OrderEventDispatcher[F] = new OrderEventDispatcher[F] {
    override def dispatch(events: Chain[OrderEvent]): F[Unit] =
      events.traverse_(e => Sync[F].delay({
        println("Dispatching event: " + e)
      }))

    override def tell(
      orderId: String, detail: OrderEventDetail)(implicit T: Tell[F, Chain[OrderEvent]]): F[Unit] = {
      for {
        orderEvent <- OrderEvent(orderId, detail)
        _ <- Tell.tellF[F](Chain.one(orderEvent))
      } yield ()
    }
  }
}
