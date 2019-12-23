package hu.ksisu.imazsak.core

import hu.ksisu.imazsak.Initable

trait RedisService[F[_]] extends Initable[F] {
  def checkStatus(): F[Boolean]
}

object RedisService {
  case class RedisConfig(host: String, port: Int, password: Option[String], database: Option[Int])
}
