package com.zhpooer.ecommerce.order.infrastructure.db

import cats.{Monad, MonadError}
import cats.effect.{Bracket, Resource}
import cats.implicits._
import cats.mtl.Ask
import doobie.Transactor
import doobie.free.connection
import doobie.implicits._
import doobie.util.transactor.Strategy

trait TransactionMrg[F[_]] {
  def readOnly[A](block: Ask[F, Transactor[F]] => F[A]): F[A]
  def startTX[A](block: Ask[F, Transactor[F]] => F[A]): F[A]
}

object TransactionMrg {

  def apply[F[_]: TransactionMrg]: TransactionMrg[F] = implicitly

  def impl[F[_]: Monad: Bracket[*[_], Throwable]](
    transactor: Resource[F, Transactor[F]]): TransactionMrg[F] = new TransactionMrg[F] {

    override def startTX[A](block: Ask[F, Transactor[F]] => F[A]): F[A] = {
      transactor.use { tx =>
        val noAutoCommitTX = tx.copy(strategy0 = Strategy.void)
        val dbExecutor = for {
          _ <- connection.setAutoCommit(false).transact(noAutoCommitTX)
          dbResult <- block.apply(Ask.const(noAutoCommitTX))
          _ <- connection.commit.transact(noAutoCommitTX)
        } yield dbResult

        dbExecutor.recoverWith {
          case e =>
            connection.rollback.transact(noAutoCommitTX) >> MonadError[F, Throwable].raiseError[A](e)
        }
      }
    }

    override def readOnly[A](block: Ask[F, Transactor[F]] => F[A]): F[A] = {
      transactor.use { tx => block.apply(Ask.const(tx)) }
    }
  }
}
