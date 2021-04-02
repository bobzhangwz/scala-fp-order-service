package com.zhpooer.ecommerce.order.order

import com.zhpooer.ecommerce.order.order.model.{Address, OrderItem}

import java.time.Instant
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

sealed trait OrderEvent {
  def orderId: String
}

object OrderEvent {
  implicit val orderEventEncoder: Encoder[OrderEvent] = {
    import io.circe.generic.auto._
    deriveEncoder
  }

  implicit val orderEventDecoder: Decoder[OrderEvent] = {
    import io.circe.generic.auto._
    deriveDecoder
  }
}

case class OrderAddressChanged(
  override val orderId: String,
  oldAddress: String,
  newAddress: String
) extends OrderEvent

case class OrderCreated(
  override val orderId: String,
  totalPrice: BigDecimal,
  address: Address,
  items: List[OrderItem],
  createdAt: Instant
) extends OrderEvent

case class OrderPaid(override val orderId: String) extends OrderEvent

case class OrderProductChanged(
  override val orderId: String, productId: String, originCount: Int, newCount: Int
) extends OrderEvent
