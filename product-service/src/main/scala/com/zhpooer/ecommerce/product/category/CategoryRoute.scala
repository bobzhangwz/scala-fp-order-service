package com.zhpooer.ecommerce.product.category

import cats.effect.Sync
import com.zhpooer.ecommerce.infrastructure.ErrorHandler
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import cats.implicits._
import com.zhpooer.ecommerce.product.category.CategoryCommand.CreateCategoryCommand
import com.zhpooer.ecommerce.product.category.repr.CategoryRepr
import io.circe.JsonObject
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._

object CategoryRoute {
  def all[F[_]: Sync](categoryAppService: CategoryAppService[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    import org.http4s.circe.CirceEntityCodec._

    val categoryErrorHandler = ErrorHandler[F, CategoryError].handle {
      case e: CategoryError.CategoryNotFound => NotFound(e.message)
    }

    HttpRoutes.of[F] {
      case GET -> Root / "categories" / id =>
        categoryErrorHandler { implicit raise =>
          implicit val categoryReprDecoder = deriveEncoder[CategoryRepr]
          categoryAppService.getById(id) >>= {Ok(_)}
        }

      case req @ POST -> Root / "categories" =>
        implicit val commandDecoder = deriveDecoder[CreateCategoryCommand]
        for {
          command <- req.as[CreateCategoryCommand]
          category <- categoryAppService.create(command)
          resp <- Ok(
            JsonObject("id" -> category.id.asJson)
          )
        } yield resp
    }
  }
}
