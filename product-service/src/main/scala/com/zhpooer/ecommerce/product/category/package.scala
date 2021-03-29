package com.zhpooer.ecommerce.product

import com.zhpooer.ecommerce.infrastructure.event.{DomainEvent, DomainEventDispatcher}

package object category {
  type CategoryDomainEvent = DomainEvent[CategoryEvent]
  type CategoryEventDispatcher[F[_]] = DomainEventDispatcher[F, CategoryEvent]
}
