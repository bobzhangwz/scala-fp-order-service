package com.zhpooer.ecommerce.product.category

sealed trait CategoryError extends Throwable {
  def message: String
}

object CategoryError {
  case class CategoryNotFound(categoryId: String) extends CategoryError {
    override val message: String = "没有找到产品目录"
  }
}

