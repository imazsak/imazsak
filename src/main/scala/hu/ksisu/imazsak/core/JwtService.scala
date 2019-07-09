package hu.ksisu.imazsak.core

import hu.ksisu.imazsak.Initable
import spray.json.JsObject

trait JwtService[F[_]] extends Initable[F] {
  def decode(token: String): F[Option[JsObject]]
  def validateAndDecode(token: String): F[Option[JsObject]]
  def validate(token: String): F[Boolean]
}
