package com.zhpooer.ecommerce.infrastructure

trait IdGenerator[F[_], A] {
  def generateId: F[A]
}

object IdGenerator {
  def apply[F[_], A](implicit I: IdGenerator[F, A]): IdGenerator[F, A] = I
}
