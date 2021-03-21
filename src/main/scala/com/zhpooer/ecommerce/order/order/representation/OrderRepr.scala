package com.zhpooer.ecommerce.order.order.representation

import com.zhpooer.ecommerce.order.order.model.{Address, OrderStatus}

import java.time.Instant

case class OrderRepr(
  id: String,
  items: List[OrderItemRepr],
  totalPrice: BigDecimal,
  status: OrderStatus,
  address: Address,
  createdAt: Instant
)

case class OrderItemRepr(
  productId: String,
  count: Int,
  itemPrice: BigDecimal
)
