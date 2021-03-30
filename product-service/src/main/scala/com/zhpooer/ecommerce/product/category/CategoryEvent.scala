package com.zhpooer.ecommerce.product.category

trait CategoryEvent

object CategoryEvent {
  case class CategoryCreated(name: String, description: String) extends CategoryEvent
}

