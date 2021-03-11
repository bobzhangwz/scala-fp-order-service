 package com.zhpooer.ecommerce.order.infrastructure.db

import ciris.Secret

case class DBConfig(url: String, driver: String, user: String, password: Secret[String])
