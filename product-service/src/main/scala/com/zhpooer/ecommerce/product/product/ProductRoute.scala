package com.zhpooer.ecommerce.product.product

import cats.data.EitherT
import cats.effect.{Concurrent, ContextShift, Timer}
import cats.implicits._
import com.zhpooer.ecommerce.product.product.ProductCommand.{ProductCreateCommand, UpdateProductNameCommand}
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s.Http4sServerInterpreter

class ProductRoute[F[_]: Concurrent: Timer: ContextShift](productAppService: ProductAppService[EitherT[F, ProductError, *]]) {
  case class ProductId(productId: String)
  val createProductEndpoint: Endpoint[ProductCreateCommand, ProductError, ProductId, Any] =
    endpoint.post.in("products")
      .in(jsonBody[ProductCreateCommand])
      .errorOut(
        oneOf[ProductError](
          statusMapping(StatusCode.NotFound, jsonBody[ProductError.ProductNotFound])
        )
      )
      .out(jsonBody[ProductId])

  val updateProductNameEndpoint: Endpoint[(String, UpdateProductNameCommand), ProductError, Unit, Any] =
    endpoint.put.in("products" / path[String]("productId") / "name")
      .in(jsonBody[UpdateProductNameCommand])
      .errorOut(
        oneOf[ProductError](
          statusMapping(StatusCode.NotFound, jsonBody[ProductError.ProductNotFound])
        )
      )

  val all: HttpRoutes[F] = {
    val route1 = Http4sServerInterpreter.toRoutes(createProductEndpoint){ command =>
      productAppService.create(command).map(p => ProductId(p.id)).value
    }
    val route2 = Http4sServerInterpreter.toRoutes(updateProductNameEndpoint){
      case (productId, command) =>
        productAppService.updateProductName(productId, command).value
    }

    route1 <+> route2
  }


}
