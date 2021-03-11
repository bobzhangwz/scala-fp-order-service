package com.zhpooer.ecommerce.order.configuration

import com.zhpooer.ecommerce.order.infrastructure.db.DBConfig
import ciris.ConfigValue
import cats.effect.Async
import cats.effect.ContextShift
import ciris.ConfigKey
import cats.implicits._

case class AppConfig(
  apiConfig: ApiConfig,
  dbConfig: DBConfig
)

case class ApiConfig(endpoint: String, port: Int)


class ConfigLoader(envMap: Map[String, String]) {
  def load[F[_]: Async: ContextShift]: F[AppConfig] = appConfig.load

  def appConfig: ConfigValue[AppConfig] = (apiConfig, dbConfig).mapN(AppConfig.apply)

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
      endpoint <- fromEnv("API_ENDPOINT")
    } yield ApiConfig(endpoint, port)

  def fromEnv(name: String): ConfigValue[String] = {
    val key = ConfigKey.env(name)
    envMap.get(name).fold(ConfigValue.missing[String](key))(ConfigValue.loaded[String](key, _))
  }

}
