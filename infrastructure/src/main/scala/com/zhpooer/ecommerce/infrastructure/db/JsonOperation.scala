package com.zhpooer.ecommerce.infrastructure.db

import cats.implicits._
import doobie._
import io.circe._
import io.circe.syntax._

import scala.reflect.runtime.universe.TypeTag

trait JsonOperation {
  def jsonGet[T: Decoder: TypeTag]: Get[T] = Get[String].temap[T](jsonStr => {
    parser.parse(jsonStr).flatMap(_.as[T]).leftMap(_.getMessage)
  })
  def jsonPut[T: Encoder]: Put[T] = Put[String].contramap[T](_.asJson.noSpaces)
}

object JsonOperation extends JsonOperation
