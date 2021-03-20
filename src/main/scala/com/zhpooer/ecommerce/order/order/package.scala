package com.zhpooer.ecommerce.order

import cats.effect.{Sync, Timer}
import com.zhpooer.ecommerce.infrastructure.db.TransactionMrg
import com.zhpooer.ecommerce.infrastructure.event.{DomainEvent, DomainEventDispatcher}
import software.amazon.awssdk.services.sns.SnsClient

package object order {
  type OrderDomainEvent = DomainEvent[OrderEvent]
  type OrderEventDispatcher[F[_]] = DomainEventDispatcher[F, OrderEvent]

  object alg {
    def orderAlg[F[_]: Timer: Sync: TransactionMrg](snsClient: SnsClient, orderEventPublisherArn: String): OrderAppService[F] = {
      implicit val orderRepositoryImpl = OrderRepository.impl[F]
      implicit val orderIdGenImpl = OrderIdGen.impl[F]
      implicit val orderEventDispatcherImpl = DomainEventDispatcher.impl[F, OrderEvent](snsClient, orderEventPublisherArn)
      OrderAppService.impl[F]
    }
  }
}
