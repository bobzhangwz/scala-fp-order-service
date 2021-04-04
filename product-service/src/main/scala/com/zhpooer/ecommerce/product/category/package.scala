package com.zhpooer.ecommerce.product

import cats.data.Chain
import com.zhpooer.ecommerce.infrastructure.event.{DomainEvent, EventDispatcher}

package object category {
  type CategoryDomainEvents = Chain[DomainEvent[CategoryEvent]]
  type CategoryEventDispatcher[F[_]] = EventDispatcher[F, CategoryEvent]
}
