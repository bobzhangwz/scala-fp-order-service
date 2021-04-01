package com.zhpooer.ecommerce.infrastructure

import cats.data.WriterT
import cats.tagless.FunctorK
import cats.tagless.implicits._
import cats.{Applicative, Monoid}

package object tools {
  def beWriterT[F[_] : Applicative, A[_[_]], E: Monoid](
    implicit af: A[F], f: FunctorK[A]
  ): A[WriterT[F, E, *]] = af.mapK(WriterT.liftK[F, E])
}
