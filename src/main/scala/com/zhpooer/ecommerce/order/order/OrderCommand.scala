package com.zhpooer.ecommerce.order.order

import com.zhpooer.ecommerce.order.order.model.Address

object OrderCommand {
  case class CreateOrderCommand(
    items: List[OrderItemCommand],
    address: Address
  )

  case class OrderItemCommand(
    productId: String,
    count: Int,
    itemPrice: BigDecimal
  )
}

