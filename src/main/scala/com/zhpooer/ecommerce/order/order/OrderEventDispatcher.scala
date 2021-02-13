package com.zhpooer.ecommerce.order.order

import cats.data.Chain
import cats.effect.Sync
import cats.implicits._

trait OrderEventDispatcher[F[_]] {
  def dispatch(events: Chain[OrderEvent]): F[Unit]
}

object OrderEventDispatcher {
  def apply[F[_]: OrderEventDispatcher]: OrderEventDispatcher[F] = implicitly

  def impl[F[_]: Sync]: OrderEventDispatcher[F] = new OrderEventDispatcher[F] {
    override def dispatch(events: Chain[OrderEvent]): F[Unit] =
      events.traverse_(e => Sync[F].delay({
        println("Dispatching event: " + e)
      }))
  }
}
