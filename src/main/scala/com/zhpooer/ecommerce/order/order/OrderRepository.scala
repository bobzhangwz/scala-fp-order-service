package com.zhpooer.ecommerce.order.order

import com.zhpooer.ecommerce.order.order.model.Order
import cats.mtl.Ask
import doobie.util.transactor.Transactor
import doobie._
import doobie.implicits._
import cats.implicits._
import cats.Monad
import cats.effect.Bracket
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import io.circe.generic.auto._

import scala.reflect.runtime.universe.TypeTag

trait OrderRepositoryAlg[F[_]] {
  def getById(id: String)(implicit A: Ask[F, Transactor[F]]): F[Option[Order]]
  def save(order: Order)(implicit A: Ask[F, Transactor[F]]): F[Unit]
}

object OrderRepository {

  def apply[F[_]: OrderRepositoryAlg]: OrderRepositoryAlg[F] = implicitly

  implicit val orderDecoder: Decoder[Order] = deriveDecoder
  implicit val orderEncoder: Encoder[Order] = deriveEncoder

  implicit def jsonGet[T: Decoder: TypeTag]: Get[T] = Get[String].temap[T](jsonStr => {
    parser.parse(jsonStr).flatMap(_.as[T]).leftMap(_.getMessage)
  })
  implicit def jsonPut[T: Encoder]: Put[T] = Put[String].contramap[T](_.asJson.noSpaces)

  def impl[F[_]: Monad: Bracket[*[_], Throwable] ]: OrderRepositoryAlg[F] = new OrderRepositoryAlg[F] {
    def getById(id: String)(implicit A: Ask[F,Transactor[F]]): F[Option[Order]] = {
      val query = sql"SELECT JSON_CONTENT FROM ORDERS WHERE ID = ${id}".query[Order].option
      A.ask.flatMap { tx => query.transact(tx) }
    }

    def save(order: Order)(implicit A: Ask[F,Transactor[F]]): F[Unit] = {
      val update = (fr"INSERT INTO ORDERS (ID, JSON_CONTENT) VALUES (${order.id}, $order)" ++
        fr" ON DUPLICATE KEY UPDATE JSON_CONTENT=${order}").update
      A.ask.flatMap { tx => update.run.transact(tx) }.void
    }
  }
}
