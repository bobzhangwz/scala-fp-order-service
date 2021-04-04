package com.zhpooer.ecommerce.infrastructure

import cats.Applicative
import cats.data.WriterT
import cats.kernel.Monoid
import cats.tagless.Derive

trait IdGenerator[F[_], A] {
  def generateId: F[A]
}

object IdGenerator {
  def apply[F[_], A](implicit I: IdGenerator[F, A]): IdGenerator[F, A] = I

  implicit def fk[A] = Derive.functorK[IdGenerator[*[_], A]]

  implicit def writerTIdGenerator[F[_]: Applicative, E: Monoid, A](
    implicit ig: IdGenerator[F, A]
  ): IdGenerator[WriterT[F, E, *], A] = {
    tools.beWriterT[F, IdGenerator[*[_], A], E]
  }
}
