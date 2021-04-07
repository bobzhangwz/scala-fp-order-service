package com.zhpooer.ecommerce.infrastructure

import cats.effect.IO
import ciris.{ConfigKey, ConfigValue}
import software.amazon.awssdk.core.client.builder.SdkClientBuilder

import java.net.URI

trait ConfigIncubator {
  def source: Map[String, String]

  def fromSource(name: String): ConfigValue[String] = {
    val key = ConfigKey.env(name)
    source.get(name).fold(ConfigValue.missing[String](key))(ConfigValue.loaded[String](key, _))
  }

  def buildAWSClient[A <: SdkClientBuilder[A, B], B](
    builder: SdkClientBuilder[A, B],
    awsOverrideEndpointKey: String = "AWS_OVERRIDE_ENDPOINT"
  ): ConfigValue[B] = {
    val awsOverrideEndpoint: ConfigValue[Option[URI]] =
      fromSource(awsOverrideEndpointKey)
        .evalMap(s => IO.delay(URI.create(s)))
        .option
    awsOverrideEndpoint
      .evalMap { maybeOverrideEndpoint =>
        IO.delay {
          maybeOverrideEndpoint.map(builder.endpointOverride)
            .getOrElse(builder)
            .build()
        }
      }
  }
}
