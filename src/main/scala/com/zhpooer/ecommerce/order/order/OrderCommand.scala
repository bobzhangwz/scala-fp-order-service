package com.zhpooer.ecommerce.order.order

import com.zhpooer.ecommerce.order.order.model.Address

object OrderCommand {
  case class CreateOrderCommand(
    items: List[CommandOrderItem],
    address: Address
  )

  case class CommandOrderItem(
    productId: String,
    count: Int,
    itemPrice: BigDecimal
  )

  case class ChangeProductCountCommand(productId: String, count: Int)

  case class PayOrderCommand(paidPrice: BigDecimal)

  case class ChangeAddressDetailCommand(detail: String)
}

