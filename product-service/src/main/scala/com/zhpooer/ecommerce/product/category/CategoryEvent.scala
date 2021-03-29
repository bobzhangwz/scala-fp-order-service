package com.zhpooer.ecommerce.product.category

trait CategoryEvent

case class CategoryCreatedEvent(name: String, description: String) extends CategoryEvent
