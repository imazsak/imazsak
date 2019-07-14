package hu.ksisu.imazsak.core

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives.{authenticateOAuth2Async, extractExecutionContext}
import akka.http.scaladsl.server.directives.Credentials
import cats.data.OptionT
import spray.json.JsString

import scala.concurrent.{ExecutionContext, Future}

trait AuthDirectives {
  import cats.instances.future._

  val jwtService: JwtService[Future]

  type AsyncAuthenticator[T] = Credentials => Future[Option[T]]

  private def validateAndGetId(token: String)(implicit ec: ExecutionContext): Future[Option[String]] = {
    OptionT(jwtService.validateAndDecode(token))
      .subflatMap(_.fields.get("id"))
      .collect {
        case JsString(id) => id
      }
      .value
  }

  protected def userAuthenticator(implicit ec: ExecutionContext): AsyncAuthenticator[String] = {
    case Credentials.Provided(token) => validateAndGetId(token)
    case _                           => Future.successful(None)
  }

  def userAuth: Directive1[String] = extractExecutionContext.flatMap { implicit executor =>
    authenticateOAuth2Async[String]("", userAuthenticator)
  }

}
