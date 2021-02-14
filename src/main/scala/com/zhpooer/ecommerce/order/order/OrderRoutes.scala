package com.zhpooer.ecommerce.order.order

import cats.effect.Sync
import cats.implicits._
import com.zhpooer.ecommerce.order.infrastructure.ErrorHandler
import com.zhpooer.ecommerce.order.order.OrderCommand.{ChangeProductCountCommand, CreateOrderCommand}
import com.zhpooer.ecommerce.order.order.model.Order
import io.circe.generic.semiauto._
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}
import org.http4s.HttpRoutes
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl

object OrderRoutes {

  def all[F[_]: Sync](orderAppService: OrderAppService[F]): HttpRoutes[F] = {
    implicit val os = orderAppService

    val dsl = new Http4sDsl[F]{}
    import dsl._

    val orderErrorHandler = ErrorHandler[F, OrderError].apply {
      case e: OrderError.OrderNotFound => NotFound(e.message)
      case e: OrderError => Conflict(e.message)
    }

    HttpRoutes.of[F]{
      case req @ POST -> Root / "orders" =>
        implicit val orderCommandDecoder: Decoder[CreateOrderCommand] = deriveDecoder
        implicit val orderCommandEntityDecoder = jsonOf[F, CreateOrderCommand]

        implicit val orderDecoder: Encoder[Order] = deriveEncoder
        implicit val orderEntityEncoder = jsonEncoderOf[F, Order]

        for {
          command <- req.as[CreateOrderCommand]
          order <- OrderAppService[F].createOrder(command)
          resp <- Ok(order)
        } yield resp

      case req @ POST -> Root / "orders" / orderId =>
        implicit val commandDecoder: Decoder[ChangeProductCountCommand] = deriveDecoder
        implicit val commandEntityDecoder = jsonOf[F, ChangeProductCountCommand]

        orderErrorHandler { implicit raise =>
          for {
            command <- req.as[ChangeProductCountCommand]
            _ <- OrderAppService[F].changeProductCount(orderId, command)
            resp <- Ok()
          } yield resp
        }

    }
  }
}

