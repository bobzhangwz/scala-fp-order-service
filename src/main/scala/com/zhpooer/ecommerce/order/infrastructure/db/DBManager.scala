package com.zhpooer.ecommerce.order.infrastructure.db
import cats.effect._
import doobie._
import doobie.hikari._

object DBManager {
  def transactor[F[_]: Async: ContextShift](blocker: Blocker): Resource[F, Transactor[F]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32)
      xa <- HikariTransactor.newHikariTransactor[F](
        "com.mysql.cj.jdbc.Driver",
        "jdbc:mysql://db:3306/db_orders",
        "mysql",
        "1234",
        ce,
        blocker
      )
    } yield xa
}
