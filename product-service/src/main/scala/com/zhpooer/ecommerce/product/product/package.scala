package com.zhpooer.ecommerce.product

import cats.effect.{Sync, Timer}
import com.zhpooer.ecommerce.infrastructure.{DomainPrelude, IdGenerator, Repository, UUIDFactory}
import com.zhpooer.ecommerce.infrastructure.db.TransactionMrg
import com.zhpooer.ecommerce.infrastructure.event.EventDispatcher
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import software.amazon.awssdk.services.sns.SnsClient

import java.util.UUID

package object product extends DomainPrelude[Product, String, ProductEvent]{
  object alg {
    trait ProductAlg[F[_]] {
      val productAppService: ProductAppService[F]
    }

    def apply[F[_]: Timer: Sync: TransactionMrg](snsClient: SnsClient, productEventPublisherArn: String): ProductAlg[F] = {

      implicit val productIdGen: DomainIdGen[F] = new IdGenerator[F, String] {
        override def generateId: F[String] = Sync[F].delay {
          UUID.randomUUID().toString
        }
      }

      implicit val productRepo: DomainRepo[F] = {
        implicit val productEncoder = deriveEncoder[Product]
        implicit val productDecoder = deriveDecoder[Product]
        Repository.of[F, Product]("PRODUCT", _.id)
      }

      implicit val productEventDispatcher: DomainEventDispatcher[F] = {
        implicit val uuidFactory = UUIDFactory.impl[F]
        implicit val eventEncoder = deriveEncoder[ProductEvent]
        EventDispatcher.impl[F, ProductEvent](snsClient, productEventPublisherArn)
      }

      new ProductAlg[F] {
        override val productAppService: ProductAppService[F] =
          ProductAppService.impl[F]
      }

    }
  }
}
