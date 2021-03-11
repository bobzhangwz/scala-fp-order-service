package com.zhpooer.ecommerce.order.infrastructure.db
import cats.effect._
import doobie._
import doobie.hikari._

object DBManager {
  def transactor[F[_]: Async: ContextShift](dbConfig: DBConfig, blocker: Blocker): Resource[F, Transactor[F]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32)
      xa <- HikariTransactor.newHikariTransactor[F](
        dbConfig.driver, dbConfig.url, dbConfig.user, dbConfig.password.value,
        ce, blocker
      )
    } yield xa
}
