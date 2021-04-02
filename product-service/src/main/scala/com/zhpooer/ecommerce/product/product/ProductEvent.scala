package com.zhpooer.ecommerce.product.product

import java.time.Instant

sealed trait ProductEvent

object ProductEvent {
  case class ProductNameUpdatedEvent(productId: String, oldName: String, newName: String) extends ProductEvent
  case class ProductCreatedEvent(
    productId: String, name: String,
    description: String, price: BigDecimal,
    createdAt: Instant
  ) extends ProductEvent
}

