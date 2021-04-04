package com.zhpooer.ecommerce.product.product

sealed trait ProductError {
  def message: String
}

object ProductError {
  case class ProductNotFound(productId: String) extends ProductError {
    override val message: String = "没有找到产品"
  }
}

