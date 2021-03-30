package com.zhpooer.ecommerce.product

import cats.data.Chain
import com.zhpooer.ecommerce.infrastructure.event.{DomainEvent, DomainEventDispatcher}

package object category {
  type CategoryDomainEvents = Chain[DomainEvent[CategoryEvent]]
  type CategoryEventDispatcher[F[_]] = DomainEventDispatcher[F, CategoryEvent]
}
