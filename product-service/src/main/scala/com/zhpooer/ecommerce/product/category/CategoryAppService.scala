package com.zhpooer.ecommerce.product.category

import cats.Monad
import cats.data.WriterT
import cats.effect.Timer
import cats.implicits._
import cats.mtl.Raise
import com.zhpooer.ecommerce.infrastructure.db.TransactionMrg
import com.zhpooer.ecommerce.infrastructure.{UUIDFactory, tools}
import com.zhpooer.ecommerce.product.category.CategoryCommand.CreateCategoryCommand
import com.zhpooer.ecommerce.product.category.CategoryError.CategoryNotFound
import com.zhpooer.ecommerce.product.category.repr.CategoryRepr

trait CategoryAppService[F[_]] {
  def create(command: CreateCategoryCommand): F[Category]

  def getById(id: String)(implicit R: Raise[F, CategoryNotFound]): F[CategoryRepr]
}

object CategoryAppService {

  def impl[F[_]: Timer: Monad: UUIDFactory: CategoryIdGen: CategoryEventDispatcher: TransactionMrg: CategoryRepo] = new CategoryAppService[F] {

    val asWriterT = WriterT.liftK[F, CategoryDomainEvents]
    implicit val uuidFacWriterT = tools.beWriterT[F, UUIDFactory, CategoryDomainEvents]
    implicit val categoryIdGenWriterWriterT = tools.beWriterT[F, CategoryIdGen, CategoryDomainEvents]

    override def create(command: CreateCategoryCommand): F[Category] = TransactionMrg[F].startTX { implicit askTX =>
      val createCategory = for {
        category <- Category.create[WriterT[F, CategoryDomainEvents, *]](command.name, command.description)
        _ <- asWriterT { CategoryRepo[F].save(category) }
      } yield category

      for {
        (events, category) <- createCategory.run
        _ <- implicitly[CategoryEventDispatcher[F]].dispatch(events)
      } yield category
    }

    def getById(id: String)(implicit R: Raise[F, CategoryNotFound]): F[CategoryRepr] = TransactionMrg[F].readOnly { implicit tx =>
      for {
        maybeCategory <- CategoryRepo[F].getById(id)
        category <- maybeCategory.fold(R.raise[CategoryNotFound, Category](CategoryNotFound(id)))(_.pure[F])
      } yield category match {
        case Category(id, name, desc, createdAt) => CategoryRepr(id, name, desc, createdAt)
      }
    }
  }
}