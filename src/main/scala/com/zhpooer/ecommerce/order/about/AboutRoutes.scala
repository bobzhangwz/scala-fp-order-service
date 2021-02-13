package com.zhpooer.ecommerce.order.about

import com.zhpooer.ecommerce.order.infrastructure.env.EnvironmentAlg
import cats.implicits._
import com.zhpooer.ecommerce.order.infrastructure.env.Environment
import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import io.circe.generic.semiauto._
import org.http4s.EntityEncoder
import io.circe.Encoder
import org.http4s.circe._

case class AboutRepresentation(
  appName: String,
  buildNumber: String,
  buildTime: String,
  deployTime: String,
  gitRevision: String,
  gitBranch: String,
  environment: String
)

trait AboutAlg[F[_]] {
  def get: F[AboutRepresentation]
}

object AboutAlg {
  def impl[F[_]: Sync: EnvironmentAlg]: AboutAlg[F] = new AboutAlg[F] {
    def get: F[AboutRepresentation] = {
      for {
        env <- Environment[F].env
        appName <- getFromEnv(env, "appName")
        buildNumber <- getFromEnv(env, "buildNumber")
        buildTime <- getFromEnv(env, "buildTime")
        deployTime <- getFromEnv(env, "deployTime")
        gitRevision <- getFromEnv(env, "gitRevision")
        gitBranch <- getFromEnv(env, "gitBranch")
        environment <- getFromEnv(env, "environment")
      } yield AboutRepresentation(
        appName, buildNumber, buildTime, deployTime,
        gitRevision, gitBranch, environment
      )
    }

    def getFromEnv(env: Map[String, String], key: String): F[String] = {
      Sync[F].fromOption(env.get(key), new Error(s"$key is not exist in Environment"))
    }
  }
}

object AboutRoutes {

  implicit val aboutReprEncoder: Encoder[AboutRepresentation] = deriveEncoder
  implicit def aboutReprEntityDecoder[F[_]: Sync]: EntityEncoder[F, AboutRepresentation] = jsonEncoderOf

  def all[F[_]: Sync](A: AboutAlg[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "about" =>
        A.get >>= {Ok(_)}
    }
  }
}

