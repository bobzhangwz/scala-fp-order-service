package com.zhpooer.ecommerce.order.order.representation

import com.zhpooer.ecommerce.order.order.model.{Address, OrderStatus}

import java.time.Instant

case class OrderSummaryRepr(
  id: String,
  totalPrice: BigDecimal,
  status: OrderStatus,
  address: Address,
  createdAt: Instant
)
