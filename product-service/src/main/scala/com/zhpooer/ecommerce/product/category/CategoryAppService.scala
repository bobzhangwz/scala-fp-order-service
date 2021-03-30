package com.zhpooer.ecommerce.product.category

import cats.{Applicative, Monad, Monoid}
import cats.data.WriterT
import cats.effect.Timer
import com.zhpooer.ecommerce.infrastructure.UUIDFactory
import com.zhpooer.ecommerce.infrastructure.db.TransactionMrg
import com.zhpooer.ecommerce.product.category.CategoryCommand.CreateCategoryCommand
import cats.tagless.implicits._
import cats.implicits._
import cats.tagless.FunctorK

trait CategoryAppService[F[_]] {
  def create(command: CreateCategoryCommand): F[Category]
}

object CategoryAppService {

  def convertToWriter[F[_] : Applicative, A[_[_]], E](
    implicit af: A[F], f: FunctorK[A], mm: Monoid[E]
  ): A[WriterT[F, E, *]] = af.mapK(WriterT.liftK[F, E])

  def impl[F[_]: Timer: Monad: UUIDFactory: CategoryIdGen: CategoryEventDispatcher: TransactionMrg: CategoryRepo] = new CategoryAppService[F] {

    val writerT = WriterT.liftK[F, CategoryDomainEvents]
    implicit val uuidFac = convertToWriter[F, UUIDFactory, CategoryDomainEvents]
    implicit val categoryIdGenWriter = convertToWriter[F, CategoryIdGen, CategoryDomainEvents]

    override def create(command: CreateCategoryCommand): F[Category] = TransactionMrg[F].startTX { implicit askTX =>
      val createCategory = for {
        category <- Category.create[WriterT[F, CategoryDomainEvents, *]](command.name, command.description)
        _ <- writerT {
          CategoryRepo[F].save(category)
        }
      } yield category

      for {
        (events, category) <- createCategory.run
        _ <- implicitly[CategoryEventDispatcher[F]].dispatch(events)
      } yield category
    }
  }
}