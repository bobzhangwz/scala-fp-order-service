package com.zhpooer.ecommerce.order.configuration

import com.zhpooer.ecommerce.infrastructure.db.DBConfig
import ciris.ConfigValue
import cats.effect.Async
import cats.effect.ContextShift
import ciris.ConfigKey
import cats.implicits._
import java.net.URI
import cats.effect.IO
import software.amazon.awssdk.services.sns.SnsClient

case class AppEnv(
  apiConfig: ApiConfig,
  dbConfig: DBConfig,
  orderEventPublisherArn: String,
  snsClient: SnsClient
)

case class ApiConfig(endpoint: String, port: Int)

class ConfigLoader(envMap: Map[String, String]) {
  def load[F[_]: Async: ContextShift]: F[AppEnv] = appConfig.load

  def appConfig: ConfigValue[AppEnv] =
    (apiConfig, dbConfig, orderEventPublisherArn, snsClient).mapN(AppEnv.apply)

  def orderEventPublisherArn: ConfigValue[String] = fromEnv("ORDER_EVENT_PUBLISHER_ARN")

  def snsClient: ConfigValue[SnsClient] =
    fromEnv("AWS_OVERRIDE_ENDPOINT")
      .evalMap(s => IO.delay(URI.create(s)))
      .option
      .evalMap { maybeOverrideEndpoint =>
        val builder = SnsClient.builder()
        IO.delay {
          maybeOverrideEndpoint.map(builder.endpointOverride(_))
            .getOrElse(builder)
            .build()
        }
      }

  def dbConfig: ConfigValue[DBConfig] = {
    for {
      url      <- fromEnv("DB_URL")
      driver   <- fromEnv("DB_DRIVER")
      user     <- fromEnv("DB_USER")
      password <- fromEnv("DB_PASSWORD").secret
    } yield DBConfig(url, driver, user, password)
  }

  def apiConfig: ConfigValue[ApiConfig] =
    for {
      port     <- fromEnv("API_PORT").as[Int].default(8081)
      endpoint <- fromEnv("API_ENDPOINT").default("0.0.0.0")
    } yield ApiConfig(endpoint, port)

  def fromEnv(name: String): ConfigValue[String] = {
    val key = ConfigKey.env(name)
    envMap.get(name).fold(ConfigValue.missing[String](key))(ConfigValue.loaded[String](key, _))
  }

}
