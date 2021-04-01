package com.zhpooer.ecommerce.product.category.repr

import java.time.Instant

case class CategoryRepr(
  id: String, name: String, description: String, createdAt: Instant
)
