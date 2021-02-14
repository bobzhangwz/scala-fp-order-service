package com.zhpooer.ecommerce.order.order

sealed trait OrderError extends Throwable {
  def status: Int
  def message: String
}

object OrderError {
  case class OrderCannotBeModified(orderId: String) extends OrderError {
    val status: Int = 409
    val message: String = "ORDER_CANNOT_BE_MODIFIED"
  }

  case class OrderNotFound(orderId: String) extends OrderError {
    val status: Int = 404
    val message: String = "ORDER_NOT_FOUND"
  }

  case class PaidPriceNotSameWithOrderPrice(orderId: String) extends OrderError {
    val status: Int = 409
    val message: String = "PAID_PRICE_NOT_SAME_WITH_ORDER_PRICE"
  }

  case class ProductNotInOrder(productId: String, orderId: String) extends OrderError {
    val status: Int = 409
    val message: String = "PRODUCT_NOT_IN_ORDER"
  }
}
