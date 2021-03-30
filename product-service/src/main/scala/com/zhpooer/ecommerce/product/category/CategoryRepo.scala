package com.zhpooer.ecommerce.product.category

import cats.effect.Bracket
import cats.mtl.Ask
import com.zhpooer.ecommerce.infrastructure.db.JsonOperation
import doobie.Transactor
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import doobie.implicits._
import cats.implicits._

trait CategoryRepo[F[_]] {
  def getById(id: String)(implicit A: Ask[F, Transactor[F]]): F[Option[Category]]
  def save(category: Category)(implicit A: Ask[F, Transactor[F]]): F[Unit]
}

object CategoryRepo {
  def apply[F[_]: CategoryRepo]: CategoryRepo[F] = implicitly

  implicit val categoryDecoder: Decoder[Category] = deriveDecoder
  implicit val categoryEncoder: Encoder[Category] = deriveEncoder

  def impl[F[_]: Bracket[*[_], Throwable] ]: CategoryRepo[F] = new CategoryRepo[F] with JsonOperation {
    implicit val categoryPut = jsonPut[Category]
    implicit val categoryGet = jsonGet[Category]

    override def getById(id: String)(implicit A: Ask[F, Transactor[F]]): F[Option[Category]] = {
      val query = sql"SELECT JSON_CONTENT FROM CATEGORY WHERE ID = ${id}".query[Category].option
      A.ask.flatMap { tx => query.transact(tx) }
    }

    override def save(category: Category)(implicit A: Ask[F, Transactor[F]]): F[Unit] = {
      val update = (fr"INSERT INTO CATEGORY (ID, JSON_CONTENT) VALUES (${category.id}, $category)" ++
        fr" ON DUPLICATE KEY UPDATE JSON_CONTENT=${category}").update
      A.ask.flatMap { tx => update.run.transact(tx) }.void
    }
  }
}
