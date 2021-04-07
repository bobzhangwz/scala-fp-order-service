package com.zhpooer.ecommerce.order.configuration

import cats.effect.{Async, ContextShift}
import cats.implicits._
import ciris.ConfigValue
import com.zhpooer.ecommerce.infrastructure.ConfigIncubator
import com.zhpooer.ecommerce.infrastructure.db.DBConfig
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sqs.SqsClient

case class AppEnv(
  apiConfig: ApiConfig,
  dbConfig: DBConfig,
  orderEventPublisherArn: String,
  orderEventListenerUrl: String,
  snsClient: SnsClient,
  sqsClient: SqsClient
)

case class ApiConfig(endpoint: String, port: Int)

class ConfigLoader(envMap: Map[String, String]) extends ConfigIncubator {

  override def source: Map[String, String] = envMap

  def load[F[_]: Async: ContextShift]: F[AppEnv] = {
    val appConfig = for {
      orderEventPubArn <- fromSource("ORDER_EVENT_PUB_ARN")
      orderEventListenerUrl <- fromSource("ORDER_EVENT_LISTENER_URL")
      snsClient <- buildAWSClient(SnsClient.builder())
      sqsClient <- buildAWSClient(SqsClient.builder())
      _dbConfig <- dbConfig
      _apiConfig <- apiConfig
    } yield AppEnv(
      apiConfig = _apiConfig, dbConfig = _dbConfig,
      orderEventPublisherArn = orderEventPubArn,
      orderEventListenerUrl = orderEventListenerUrl,
      snsClient = snsClient, sqsClient = sqsClient
    )

    appConfig.load
  }

  def dbConfig: ConfigValue[DBConfig] = {
    for {
      url      <- fromSource("DB_URL")
      driver   <- fromSource("DB_DRIVER")
      user     <- fromSource("DB_USER")
      password <- fromSource("DB_PASSWORD").secret
    } yield DBConfig(url, driver, user, password)
  }

  def apiConfig: ConfigValue[ApiConfig] =
    for {
      port     <- fromSource("API_PORT").as[Int].default(8081)
      endpoint <- fromSource("API_ENDPOINT").default("0.0.0.0")
    } yield ApiConfig(endpoint, port)

}
