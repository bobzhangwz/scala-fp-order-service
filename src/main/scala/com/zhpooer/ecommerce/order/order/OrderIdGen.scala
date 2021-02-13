package com.zhpooer.ecommerce.order.order

import cats.effect.Sync

import java.util.UUID

trait OrderIdGenAlg[F[_]] {
  def gen: F[String]
}

object OrderIdGen {
  def apply[F[_]: OrderIdGenAlg]: OrderIdGenAlg[F] = implicitly

  def impl[F[_]: Sync]: OrderIdGenAlg[F] = new OrderIdGenAlg[F] {
    override def gen: F[String] = Sync[F].delay(UUID.randomUUID().toString)
  }
}
