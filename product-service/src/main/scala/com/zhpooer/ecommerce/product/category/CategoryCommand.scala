package com.zhpooer.ecommerce.product.category

object CategoryCommand {
  case class CreateCategoryCommand(name: String, description: String)
}
