package com.zhpooer.ecommerce.infrastructure

import cats.effect.Bracket
import cats.mtl.Ask
import com.zhpooer.ecommerce.infrastructure.db.JsonOperation
import doobie.Transactor
import io.circe.{Decoder, Encoder}
import cats.implicits._
import doobie.implicits._
import doobie.util.fragment.Fragment

import scala.reflect.runtime.universe.TypeTag

trait Repository[F[_], E] {
  def getById(id: String)(implicit A: Ask[F, Transactor[F]]): F[Option[E]]
  def save(category: E)(implicit A: Ask[F, Transactor[F]]): F[Unit]
}

object Repository {
  def apply[F[_], E](implicit R: Repository[F, E]) = R

  def of[
    F[_]: Bracket[*[_], Throwable], E: Encoder: Decoder: TypeTag
  ](tableName: String, getId: E => String): Repository[F, E] = new Repository[F, E] with JsonOperation  {
    implicit val dbEPut = jsonPut[E]
    implicit val dbEGet = jsonGet[E]
    val table = Fragment.const(tableName)

    override def getById(id: String)(implicit A: Ask[F, Transactor[F]]): F[Option[E]] = {
      val query = fr"SELECT JSON_CONTENT FROM" ++ table ++ fr"WHERE ID = ${id}"
      A.ask.flatMap { tx => query.query[E].option.transact(tx) }
    }

    override def save(e: E)(implicit A: Ask[F, Transactor[F]]): F[Unit] = {
      val id = getId(e)
      val update = (fr"INSERT INTO" ++ table ++ fr"(ID, JSON_CONTENT) VALUES (${id}, $e)" ++
        fr" ON DUPLICATE KEY UPDATE JSON_CONTENT=${e}").update
      A.ask.flatMap { tx => update.run.transact(tx) }.void
    }
  }
}
