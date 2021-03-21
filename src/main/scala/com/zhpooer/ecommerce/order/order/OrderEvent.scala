package com.zhpooer.ecommerce.order.order

import com.zhpooer.ecommerce.order.order.model.{Address, OrderItem}

import java.time.Instant
import io.circe.Encoder
import io.circe.generic.semiauto._
import io.circe.generic.auto._

sealed trait OrderEvent

object OrderEvent {
  implicit val orderEventDecoder: Encoder[OrderEvent] = deriveEncoder
}

case class OrderAddressChanged(
  oldAddress: String,
  newAddress: String
) extends OrderEvent

case class OrderCreated(
  totalPrice: BigDecimal,
  address: Address,
  items: List[OrderItem],
  createdAt: Instant
) extends OrderEvent

case object OrderPaid extends OrderEvent

case class OrderProductChanged(
  productId: String, originCount: Int, newCount: Int
) extends OrderEvent
