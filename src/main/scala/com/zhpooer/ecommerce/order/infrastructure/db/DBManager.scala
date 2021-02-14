package com.zhpooer.ecommerce.order.infrastructure.db
import cats.effect._
import doobie._
import doobie.hikari._

object DBManager {
  def transactor[F[_]: Async: ContextShift](blocker: Blocker): Resource[F, Transactor[F]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32)
      xa <- HikariTransactor.newHikariTransactor[F](
        "org.h2.Driver",
        "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        "sa",
        "",
        ce,
        blocker
      )
    } yield xa
}
