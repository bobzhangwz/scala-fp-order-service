package com.zhpooer.ecommerce.order

import cats.effect.{Sync, Timer}
import com.zhpooer.ecommerce.infrastructure.db.TransactionMrg
import software.amazon.awssdk.services.sns.SnsClient

package object order {
  object alg {
    def orderAlg[F[_]: Timer: Sync: TransactionMrg](snsClient: SnsClient, orderEventPublisherArn: String): OrderAppService[F] = {
      implicit val orderRepositoryImpl = OrderRepository.impl[F]
      implicit val orderIdGenImpl = OrderIdGen.impl[F]
      implicit val orderEventDispatcherImpl = OrderEventDispatcher.impl[F](snsClient, orderEventPublisherArn)
      OrderAppService.impl[F]
    }
  }
}
