package com.zhpooer.ecommerce.order

import cats.effect.Bracket
import com.zhpooer.ecommerce.order.infrastructure.db.TransactionMrg

package object order {
  object implicits {
    implicit def orderAppService[F[_]: Bracket[*[_], Throwable]: TransactionMrg]: OrderAppService[F] = {
      ???
    }
  }
}
