package com.zhpooer.ecommerce.product

import cats.effect.{Async, ContextShift}
import ciris.ConfigValue
import com.zhpooer.ecommerce.infrastructure.ConfigIncubator
import com.zhpooer.ecommerce.infrastructure.db.DBConfig
import software.amazon.awssdk.services.sns.SnsClient
import cats.implicits._

case class ProductAppEnv(
  listenedPort: Int,
  productEventPublisherArn: String,
  snsClient: SnsClient,
  productDBConfig: DBConfig
)

class AppEnvLoader(envMap: Map[String, String]) {
  val incubator = new ConfigIncubator(envMap)
  import incubator._

  def load[F[_]: Async: ContextShift]: F[ProductAppEnv] = {
    val config = for {
      listenedPort <- fromEnv("API_PORT").as[Int].default(8082)
      snsClient <- buildAWSClient(SnsClient.builder(), "AWS_OVERRIDE_ENDPOINT")
      productEventPublisherArn <- fromEnv("PRODUCT_EVENT_PUB_ARN")
      productDBConfig <- dbConfig
    } yield ProductAppEnv(
      listenedPort,productEventPublisherArn, snsClient, productDBConfig
    )
    config.load[F]
  }

  def dbConfig: ConfigValue[DBConfig] = {
    for {
      url      <- fromEnv("DB_URL")
      driver   <- fromEnv("DB_DRIVER")
      user     <- fromEnv("DB_USER")
      password <- fromEnv("DB_PASSWORD").secret
    } yield DBConfig(url, driver, user, password)
  }

}
