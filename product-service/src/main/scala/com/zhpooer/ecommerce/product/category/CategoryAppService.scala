package com.zhpooer.ecommerce.product.category

import cats.Monad
import cats.data.{Chain, WriterT}
import cats.effect.Timer
import cats.implicits._
import cats.mtl.Raise
import com.zhpooer.ecommerce.infrastructure.db.TransactionMrg
import com.zhpooer.ecommerce.infrastructure.Repository
import com.zhpooer.ecommerce.product.category.CategoryCommand.CreateCategoryCommand
import com.zhpooer.ecommerce.product.category.CategoryError.CategoryNotFound
import com.zhpooer.ecommerce.product.category.repr.CategoryRepr

trait CategoryAppService[F[_]] {
  def create(command: CreateCategoryCommand): F[Category]

  def getById(id: String)(implicit R: Raise[F, CategoryNotFound]): F[CategoryRepr]
}

object CategoryAppService {

  def impl[
    F[_]: Timer: Monad: DomainIdGen: DomainEventDispatcher: TransactionMrg: DomainRepo
  ]: CategoryAppService[F] = new CategoryAppService[F] {

    override def create(command: CreateCategoryCommand): F[Category] = TransactionMrg[F].startTX { implicit askTX =>
      val createCategory = for {
        category <- Category.create[WriterT[F, Chain[CategoryEvent], *]](command.name, command.description)
        _ <- Repository[F, Category].save(category).asEventsWriter
      } yield category

      for {
        (events, category) <- createCategory.run
        _ <- implicitly[DomainEventDispatcher[F]].dispatch(events)
      } yield category
    }

    def getById(id: String)(implicit R: Raise[F, CategoryNotFound]): F[CategoryRepr] = TransactionMrg[F].readOnly { implicit tx =>
      for {
        maybeCategory <- Repository[F, Category].getById(id)
        category <- maybeCategory.fold(R.raise[CategoryNotFound, Category](CategoryNotFound(id)))(_.pure[F])
      } yield category match {
        case Category(id, name, desc, createdAt) => CategoryRepr(id, name, desc, createdAt)
      }
    }
  }
}