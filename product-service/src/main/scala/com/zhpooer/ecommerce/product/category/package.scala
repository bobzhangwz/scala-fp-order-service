package com.zhpooer.ecommerce.product

import cats.effect.{Sync, Timer}
import com.zhpooer.ecommerce.infrastructure.{DomainPrelude, IdGenerator, Repository, UUIDFactory}
import com.zhpooer.ecommerce.infrastructure.db.TransactionMrg
import com.zhpooer.ecommerce.infrastructure.event.EventDispatcher
import io.circe.generic.semiauto._
import software.amazon.awssdk.services.sns.SnsClient

import java.util.UUID

package object category extends DomainPrelude[Category, String, CategoryEvent]{
  object alg {
    trait CategoryAlg[F[_]] {
      val categoryAppService: CategoryAppService[F]
    }

    def apply[F[_]: Timer: Sync: TransactionMrg](snsClient: SnsClient, categoryEventPublisherArn: String): CategoryAlg[F] = {

      implicit val categoryIdGen: DomainIdGen[F] = new IdGenerator[F, String] {
        override def generateId: F[String] = Sync[F].delay {
          UUID.randomUUID().toString
        }
      }

      implicit val productRepo: DomainRepo[F] = {
        implicit val productEncoder = deriveEncoder[Category]
        implicit val productDecoder = deriveDecoder[Category]
        Repository.of[F, Category]("CATEGORY", _.id)
      }

      implicit val productEventDispatcher: DomainEventDispatcher[F] = {
        implicit val uuidFactory = UUIDFactory.impl[F]
        implicit val eventEncoder = deriveEncoder[CategoryEvent]
        EventDispatcher.impl[F, CategoryEvent](snsClient, categoryEventPublisherArn)
      }

      new CategoryAlg[F] {
        override val categoryAppService: CategoryAppService[F] =
          CategoryAppService.impl[F]
      }

    }
  }
}
