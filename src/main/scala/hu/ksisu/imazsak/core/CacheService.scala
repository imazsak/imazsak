package hu.ksisu.imazsak.core

import hu.ksisu.imazsak.Initable
import spray.json.JsonFormat

import scala.concurrent.duration.FiniteDuration

trait CacheService[F[_]] extends Initable[F] {
  def checkStatus(): F[Boolean]

  def findOrSet[T](key: String, ttl: Option[FiniteDuration] = None)(valueF: => F[T])(
      implicit format: JsonFormat[T]
  ): F[T]

  def remove(key: String): F[Unit]
}
