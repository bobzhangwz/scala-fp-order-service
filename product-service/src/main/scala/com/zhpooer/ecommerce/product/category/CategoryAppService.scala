package com.zhpooer.ecommerce.product.category

import cats.{Functor, Monad}
import cats.data.Chain
import cats.effect.Timer
import cats.mtl.Tell
import com.zhpooer.ecommerce.infrastructure.UUIDFactory
import com.zhpooer.ecommerce.infrastructure.db.TransactionMrg
import com.zhpooer.ecommerce.product.category.CategoryCommand.CreateCategoryCommand
import cats.implicits._

trait CategoryAppService[F[_]] {
  def create(command: CreateCategoryCommand): F[Category]
}

object CategoryAppService {
  def impl[F[_] : Timer : Monad : UUIDFactory : CategoryIdGen : CategoryEventDispatcher : TransactionMrg : CategoryRepo] = new CategoryAppService[F] {
    implicit val tellInstance: Tell[F, Chain[CategoryDomainEvent]] = new Tell[F, Chain[CategoryDomainEvent]] {
      override def functor: Functor[F] = implicitly
      override def tell(l: Chain[CategoryDomainEvent]): F[Unit] = implicitly[CategoryEventDispatcher[F]].dispatch(l)
    }

    override def create(command: CreateCategoryCommand): F[Category] = TransactionMrg[F].startTX { implicit askTX =>
      for {
        category <- Category.create[F](command.name, command.description)
        _ <- CategoryRepo[F].save(category)
      } yield category
    }
  }
}