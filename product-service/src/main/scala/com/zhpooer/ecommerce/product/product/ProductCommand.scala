package com.zhpooer.ecommerce.product.product

sealed trait ProductCommand

object ProductCommand {
  case class ProductCreateCommand(
    name: String,
    description: String,
    price: BigDecimal,
    categoryId: String
  ) extends ProductCommand

  case class UpdateProductNameCommand(
    newName: String
  ) extends ProductCommand
}

