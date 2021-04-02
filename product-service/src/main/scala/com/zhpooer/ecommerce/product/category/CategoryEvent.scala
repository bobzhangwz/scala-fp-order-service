package com.zhpooer.ecommerce.product.category

trait CategoryEvent

object CategoryEvent {
  case class CategoryCreated(
    categoryId: String, name: String, description: String) extends CategoryEvent
}

