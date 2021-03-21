package com.zhpooer.ecommerce.order.order

import cats.data.NonEmptyList
import cats.data.Validated._
import cats.effect.Sync
import cats.implicits._
import com.zhpooer.ecommerce.infrastructure.ErrorHandler
import com.zhpooer.ecommerce.order.order.OrderCommand.{ChangeProductCountCommand, CreateOrderCommand}
import com.zhpooer.ecommerce.order.order.model.{Address, Order}
import com.zhpooer.ecommerce.order.order.representation.OrderRepresentationService
import io.circe.generic.semiauto._
import io.circe.generic.auto._
import io.circe.{Decoder, Encoder}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object OrderRoutes {

  def all[F[_]: Sync](orderAppService: OrderAppService[F], orderReprService: OrderRepresentationService[F]): HttpRoutes[F] = {
    implicit val os = orderAppService

    val dsl = new Http4sDsl[F]{}
    import dsl._
    import org.http4s.circe.CirceEntityCodec._

    val orderErrorHandler = ErrorHandler[F, OrderError].apply {
      case e: OrderError.OrderNotFound => NotFound(e.message)
      case e: OrderError => Conflict(e.message)
    }

    HttpRoutes.of[F]{
      case req @ POST -> Root / "orders" =>
        implicit val orderCommandDecoder: Decoder[CreateOrderCommand] = deriveDecoder
        implicit val orderDecoder: Encoder[Order] = deriveEncoder

        val validateOrderItem = (command: CreateOrderCommand) =>
          NonEmptyList.fromList(command.items).toValidNel("Order item can't be empty")
        val validateAddress = (address: Address) => {
          NonEmptyList.fromList(address.city.toList).toValidNel("City can't be empty") product
            NonEmptyList.fromList(address.province.toList).toValidNel("Province can't be empty")
        }
        for {
          command <- req.as[CreateOrderCommand]
          resp <- validateOrderItem(command) product validateAddress(command.address) match {
            case Valid(_) => OrderAppService[F].createOrder(command) >>= (Created(_))
            case Invalid(errors) => BadRequest(errors.toList)
          }
        } yield resp

      case req @ POST -> Root / "orders" / orderId / "products" =>
        implicit val commandDecoder: Decoder[ChangeProductCountCommand] = deriveDecoder

        orderErrorHandler { implicit raise =>
          for {
            command <- req.as[ChangeProductCountCommand]
            _ <- OrderAppService[F].changeProductCount(orderId, command)
            resp <- Ok()
          } yield resp
        }

      case GET -> Root / "orders" / orderId =>
        orderErrorHandler { implicit raise =>
          orderReprService.getById(orderId) >>= (Ok(_))
        }
    }
  }
}
