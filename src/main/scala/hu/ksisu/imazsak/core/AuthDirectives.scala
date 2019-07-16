package hu.ksisu.imazsak.core

import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.AuthenticationFailedRejection.CredentialsRejected
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Directive0, Directive1}
import cats.data.OptionT
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.util.LoggerUtil.{AdminLogContext, LogContext, UserLogContext}
import hu.ksisu.imazsak.util.TracingDirectives.trace
import io.opentracing.{Span, Tracer}
import spray.json.JsString

import scala.concurrent.{ExecutionContext, Future}

trait AuthDirectives {
  this: Api =>

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

  def adminAuth: Directive0 = {
    headerValueByName("X-Admin-Key").flatMap { key =>
      if (key == "adminpass") { // todo config
        pass
      } else {
        reject(AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("", "")))
      }
    }
  }

  protected def withUserTrace[T <: LogContext](name: String, userId: String)(
      contextFactory: (String, Tracer, Span) => T
  ): Directive1[T] = {
    trace(tracer, name).map(contextFactory(userId, tracer, _))
  }

  def userAuthAndTrace(name: String): Directive1[UserLogContext] = {
    userAuth.flatMap(userId => withUserTrace(name, userId)(UserLogContext))
  }

  def adminAuthAndTrace(name: String): Directive1[AdminLogContext] = {
    adminAuth.tflatMap(_ => userAuth.flatMap(userId => withUserTrace(name, userId)(AdminLogContext)))
  }

}
