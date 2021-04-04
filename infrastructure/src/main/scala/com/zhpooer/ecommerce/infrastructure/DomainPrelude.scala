package com.zhpooer.ecommerce.infrastructure

import cats.Applicative
import cats.arrow.FunctionK
import cats.data.{Chain, WriterT}
import com.zhpooer.ecommerce.infrastructure.event.EventDispatcher

trait DomainPrelude[DomainType, IdType, EventType] {
  type DomainRepo[F[_]] = Repository[F, DomainType]
  type DomainIdGen[F[_]] = IdGenerator[F, IdType]
  type DomainEventDispatcher[F[_]] = EventDispatcher[F, EventType]

  type EventWriterT[F[_], A] = WriterT[F, Chain[EventType], A]
  def liftToEventWriterT[F[_]: Applicative]: FunctionK[F, WriterT[F, Chain[EventType], *]] =
    WriterT.liftK[F, Chain[EventType]]

  implicit class EventWriterTWrapper[F[_]: Applicative, A](f: F[A]) {
    def asEventsWriter: WriterT[F, Chain[EventType], A] = liftToEventWriterT[F].apply(f)
  }

}
