package com.zhpooer.ecommerce.order.order.model

import org.atnos.eff._
import org.atnos.eff.macros._

@eff trait OrderDsl {
  sealed trait OrderDslAdt[T]

  type _orderDsl[R] = OrderDslAdt |= R

  def create[R :_orderDsl](items: List[OrderItem], address: Address): Eff[R, Order]
  def changeProductCount[R :_orderDsl](productId: String, count: Int): Eff[R, Order]
}

