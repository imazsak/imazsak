package hu.ksisu.imazsak.core.impl

import java.security.Security
import java.time.Clock

import cats.MonadError
import hu.ksisu.imazsak.core.Errors.{InvalidJwt, WrongConfig}
import hu.ksisu.imazsak.core.JwtService
import hu.ksisu.imazsak.core.impl.JwtServiceImpl.JwtConfig
import org.bouncycastle.jce.provider.BouncyCastleProvider
import pdi.jwt.algorithms.{JwtAsymmetricAlgorithm, JwtHmacAlgorithm}
import pdi.jwt.{JwtAlgorithm, JwtSprayJson}
import spray.json._

class JwtServiceImpl[F[_]: MonadError[*[_], Throwable]](implicit config: JwtConfig) extends JwtService[F] {

  import cats.syntax.flatMap._
  import cats.syntax.functor._
  import hu.ksisu.imazsak.util.ApplicativeErrorSyntax._

  protected implicit val clock = Clock.systemDefaultZone()

  private def algo: F[JwtAlgorithm] = {
    JwtAlgorithm
      .optionFromString(config.algorithm)
      .pureOrRaise(WrongConfig("Unsupported JWT algorithm"))
  }

  override def init: F[Unit] = {
    Security.addProvider(new BouncyCastleProvider)
    algo.map(_ => ())
  }

  override def decode(token: String): F[Option[JsObject]] = {
    algo.map {
      case a: JwtHmacAlgorithm       => JwtSprayJson.decodeJson(token, config.secret, Seq(a)).toOption
      case a: JwtAsymmetricAlgorithm => JwtSprayJson.decodeJson(token, config.secret, Seq(a)).toOption
      case _                         => None
    }
  }

  override def validateAndDecode(token: String): F[Option[JsObject]] = {
    for {
      _    <- validate(token).map(_.pureUnitOrRise(InvalidJwt()))
      data <- decode(token)
    } yield data
  }

  override def validate(token: String): F[Boolean] = {
    algo.map {
      case a: JwtHmacAlgorithm       => JwtSprayJson.isValid(token, config.secret, Seq(a))
      case a: JwtAsymmetricAlgorithm => JwtSprayJson.isValid(token, config.secret, Seq(a))
      case _                         => false
    }
  }
}

object JwtServiceImpl {
  case class JwtConfig(
      algorithm: String,
      secret: String
  )
}
