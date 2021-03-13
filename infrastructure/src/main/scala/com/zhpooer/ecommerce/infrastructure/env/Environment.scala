package com.zhpooer.ecommerce.infrastructure.env

import cats.Applicative

trait EnvironmentAlg[F[_]] {
  def env: F[Map[String, String]]
}

object Environment {
  def apply[F[_]: EnvironmentAlg]: EnvironmentAlg[F] = implicitly

  def impl[F[_]: Applicative]: EnvironmentAlg[F] = new EnvironmentAlg[F] {
    def env: F[Map[String,String]] = Applicative[F].pure(sys.env)
  }
}
