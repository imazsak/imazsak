package hu.ksisu.imazsak.util

import cats.Applicative

trait DateTimeUtil[F[_]] {
  def getCurrentTimeMillis(): F[Long]
}

class DateTimeUtilImpl[F[_]: Applicative] extends DateTimeUtil[F] {
  import cats.syntax.applicative._
  override def getCurrentTimeMillis(): F[Long] = System.currentTimeMillis().pure
}
