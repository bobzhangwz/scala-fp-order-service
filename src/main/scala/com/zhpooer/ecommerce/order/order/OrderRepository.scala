package com.zhpooer.ecommerce.order.order

import com.zhpooer.ecommerce.order.order.model.Order
import cats.mtl.Ask
import doobie.util.transactor.Transactor
import doobie.implicits._
import cats.implicits._
import cats.effect.Bracket
import com.zhpooer.ecommerce.infrastructure.db.JsonOperation
import io.circe._
import io.circe.generic.semiauto._

trait OrderRepositoryAlg[F[_]] {
  def getById(id: String)(implicit A: Ask[F, Transactor[F]]): F[Option[Order]]
  def save(order: Order)(implicit A: Ask[F, Transactor[F]]): F[Unit]
}

object OrderRepository {

  def apply[F[_]: OrderRepositoryAlg]: OrderRepositoryAlg[F] = implicitly

  implicit val orderDecoder: Decoder[Order] = {
    import io.circe.generic.auto._
    deriveDecoder
  }
  implicit val orderEncoder: Encoder[Order] = {
    import io.circe.generic.auto._
    deriveEncoder
  }

  def impl[F[_]: Bracket[*[_], Throwable] ]: OrderRepositoryAlg[F] = new OrderRepositoryAlg[F] with JsonOperation {
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
