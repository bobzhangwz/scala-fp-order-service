package com.zhpooer.ecommerce.product.product

sealed trait ProductEvent

object ProductEvent {
  case class ProductNameUpdatedEvent(productId: String, oldName: String, newName: String) extends ProductEvent
}

