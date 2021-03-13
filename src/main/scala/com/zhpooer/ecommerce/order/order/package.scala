package com.zhpooer.ecommerce.order

import cats.effect.{Sync, Timer}
import com.zhpooer.ecommerce.infrastructure.db.TransactionMrg

package object order {
  object alg {
    def orderAlg[F[_]: Timer: Sync: TransactionMrg]: OrderAppService[F] = {
      implicit val orderRepositoryImpl = OrderRepository.impl[F]
      implicit val orderIdGenImpl = OrderIdGen.impl[F]
      implicit val orderEventDispatcherImpl = OrderEventDispatcher.impl[F]
      OrderAppService.impl[F]
    }
  }
}
