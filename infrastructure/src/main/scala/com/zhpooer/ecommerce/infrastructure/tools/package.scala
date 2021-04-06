package com.zhpooer.ecommerce.infrastructure

import cats.data.{EitherT, WriterT}
import cats.tagless.FunctorK
import cats.tagless.implicits._
import cats.{Applicative, Functor, Monoid}

package object tools {
  def beWriterT[F[_] : Applicative, A[_[_]], E: Monoid](
    implicit af: A[F], f: FunctorK[A]
  ): A[WriterT[F, E, *]] = af.mapK(WriterT.liftK[F, E])

  def beEitherT[F[_] : Functor, A[_[_]], E](af: A[F], f: FunctorK[A]): A[EitherT[F, E, *]] =
    f.mapK(af)(EitherT.liftK[F, E])
}
