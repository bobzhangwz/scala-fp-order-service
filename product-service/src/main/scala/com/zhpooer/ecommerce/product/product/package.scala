package com.zhpooer.ecommerce.product

import cats.data.Chain
import com.zhpooer.ecommerce.infrastructure.event.{DomainEvent, DomainEventDispatcher}

package object product {
  type ProductDomainEvents = Chain[DomainEvent[ProductEvent]]
  type ProductEventDispatcher[F[_]] = DomainEventDispatcher[F, ProductEvent]
}
