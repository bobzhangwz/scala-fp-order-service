package com.zhpooer.ecommerce.order.order.model

import cats.Applicative
import cats.implicits._
import cats.mtl.Ask
import com.zhpooer.ecommerce.order.order.OrderError
import munit.FunSuite

import java.time.Instant

class OrderSpec extends FunSuite {
  def orderAsk[F[_]: Applicative](order: Order): Ask[F, Order] = Ask.const(order)

  test("changeProductCount") {
    val order = Order(
      id = "1",
      items = List(),
      totalPrice = 200,
      status = OrderStatus.Paid,
      address = Address("", "", ""),
      createdAt = Instant.now
    )

    implicit val ask = orderAsk[Either[OrderError, *]](order)
    Order.changeProductCount[Either[OrderError, *]]("product-1", 1)
  }

}
