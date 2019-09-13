package hu.ksisu.imazsak.token

import hu.ksisu.imazsak.Errors.Response
import hu.ksisu.imazsak.Initable
import hu.ksisu.imazsak.token.TokenService.CreateTokenData
import spray.json.{JsonReader, JsonWriter}

import scala.concurrent.duration.Duration

trait TokenService[F[_]] extends Initable[F] {
  def createToken[T](data: CreateTokenData[T])(implicit w: JsonWriter[T]): F[String]
  def validateAndGetTokenData[T](tokenType: String, token: String)(implicit r: JsonReader[T]): Response[F, Option[T]]
  def revokeToken(tokenType: String, token: String): Response[F, Unit]
}

object TokenService {
  case class CreateTokenData[T](
      tokenType: String,
      data: Option[T] = None,
      expiredAfter: Option[Duration] = None,
      reusable: Boolean = false
  )
}
