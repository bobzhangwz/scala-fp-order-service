package com.zhpooer.ecommerce.order

import cats.effect.{Sync, Timer}
import com.zhpooer.ecommerce.infrastructure.UUIDFactory
import com.zhpooer.ecommerce.infrastructure.db.TransactionMrg
import com.zhpooer.ecommerce.infrastructure.event.{DomainEvent, DomainEventDispatcher}
import com.zhpooer.ecommerce.order.order.representation.OrderRepresentationService
import software.amazon.awssdk.services.sns.SnsClient

package object order {
  type OrderDomainEvent = DomainEvent[OrderEvent]
  type OrderEventDispatcher[F[_]] = DomainEventDispatcher[F, OrderEvent]

  object alg {
    trait OrderAlg[F[_]] {
      val orderAppSvc: OrderAppService[F]
      val orderReprSvc: OrderRepresentationService[F]
    }

    def apply[F[_]: Timer: Sync: TransactionMrg](snsClient: SnsClient, orderEventPublisherArn: String): OrderAlg[F] = {
      implicit val orderRepositoryImpl = OrderRepository.impl[F]
      implicit val orderIdGenImpl = OrderIdGen.impl[F]
      implicit val orderEventDispatcherImpl = DomainEventDispatcher.impl[F, OrderEvent](snsClient, orderEventPublisherArn)
      implicit val uuidFactory = UUIDFactory.impl[F]

      new OrderAlg[F] {
        override val orderAppSvc: OrderAppService[F] = OrderAppService.impl[F]
        override val orderReprSvc: OrderRepresentationService[F] = OrderRepresentationService.impl[F]
      }
    }
  }
}
