package com.zhpooer.ecommerce.order.infrastructure

import cats.mtl.Raise
import cats.{Functor, MonadError}

import scala.reflect.ClassTag

object ErrorHandler {

  def apply[F[_]: MonadError[*[_], Throwable], E <: Throwable : ClassTag]: PartialApplyErrorHandler[F, E] =
    new PartialApplyErrorHandler[F, E]

  def handleError[F[_]: MonadError[*[_], Throwable], E <: Throwable : ClassTag, A](
    errorHandler: E => F[A]
  ): (Raise[F, E] => F[A]) => F[A] = { block: (Raise[F, E] => F[A]) =>

    implicit val handler: Raise[F, E] = new Raise[F, E] {
      override def raise[E2 <: E, B](e: E2): F[B] = MonadError[F, Throwable].raiseError(e)
      override def functor: Functor[F] = implicitly
    }

    MonadError[F, Throwable].recoverWith(block.apply(handler)) {
      case e: E => errorHandler(e)
    }
  }

  class PartialApplyErrorHandler[F[_]: MonadError[*[_], Throwable], E <: Throwable : ClassTag] {
    def apply[A](errorHandler: E => F[A]):(Raise[F, E] => F[A]) => F[A] = handleError(errorHandler)
  }

}
