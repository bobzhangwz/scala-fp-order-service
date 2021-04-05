package com.zhpooer.ecommerce.product.product

import cats.Monad
import cats.data.{Chain, WriterT}
import cats.effect.Timer
import cats.mtl.Raise
import com.zhpooer.ecommerce.infrastructure.db.TransactionMrg
import com.zhpooer.ecommerce.infrastructure.Repository
import cats.implicits._

trait ProductAppService[F[_]] {
  def create(command: ProductCommand.ProductCreateCommand): F[Product]
  def updateProductName(productId: String, command: ProductCommand.UpdateProductNameCommand)(
      implicit R: Raise[F, ProductError.ProductNotFound]
    ): F[Unit]
}

object ProductAppService {
  def impl[F[_]: Monad: Timer: TransactionMrg: DomainIdGen: DomainRepo: DomainEventDispatcher]: ProductAppService[F] = new ProductAppService[F] {
    override def create(
      c: ProductCommand.ProductCreateCommand
    ): F[Product] = TransactionMrg[F].startTX { implicit askTX =>
      val createProduct: WriterT[F, Chain[ProductEvent], Product] = for {
        product <- Product.create[WriterT[F, Chain[ProductEvent], *]](
          c.name, c.description, c.price, c.categoryId
        )
        _ <- Repository[F, Product].save(product).asEventsWriter
      } yield product

      implicitly[DomainEventDispatcher[F]].dispatchAndExtract(createProduct)
    }

    override def updateProductName(
      productId: String, command: ProductCommand.UpdateProductNameCommand
    )(implicit R: Raise[F, ProductError.ProductNotFound]): F[Unit] =
      TransactionMrg[F].startTX {implicit tx =>
        val update = for {
          mayBeProduct <- Repository[F, Product].getById(productId).asEventsWriter
          product <- mayBeProduct.fold(
            R.raise[ProductError.ProductNotFound, Product](ProductError.ProductNotFound(productId))
          )(_.pure[F]).asEventsWriter
          updatedProduct <- Product.updateName[EventWriterT[F, *]](command.newName).run(product)
          _ <- Repository[F, Product].save(updatedProduct).asEventsWriter
        } yield ()

        implicitly[DomainEventDispatcher[F]].dispatchAndExtract(update)
      }
  }
}
