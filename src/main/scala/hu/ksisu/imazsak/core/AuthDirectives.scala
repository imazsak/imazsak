package hu.ksisu.imazsak.core

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import cats.data.OptionT
import cats.effect.IO
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.util.LoggerUtil.{AdminLogContext, LogContext, UserLogContext}
import hu.ksisu.imazsak.util.TracingDirectives.trace
import io.opentracing.{Span, Tracer}
import spray.json.JsString

import scala.concurrent.Future

trait AuthDirectives {
  this: Api =>

  val jwtService: JwtService[IO]

  type AsyncAuthenticator[T] = Credentials => Future[Option[T]]

  private def validateAndGetId(token: String, admin: Boolean): Future[Option[String]] = {
    OptionT(jwtService.validateAndDecode(token))
      .subflatMap(_.fields.get("id"))
      .collect {
        case JsString(id) => id
      }
      .value
      .unsafeToFuture()
  }

  protected def jwtAuthenticator(admin: Boolean): AsyncAuthenticator[String] = {
    case Credentials.Provided(token) => validateAndGetId(token, admin)
    case _                           => Future.successful(None)
  }

  def userAuth: Directive1[String] = authenticateOAuth2Async[String]("", jwtAuthenticator(admin = false))

  def adminAuth: Directive1[String] = authenticateOAuth2Async[String]("", jwtAuthenticator(admin = true))

  protected def withUserTrace[T <: LogContext](name: String, userId: String)(
      contextFactory: (String, Tracer, Span) => T
  ): Directive1[T] = {
    trace(tracer, name).map(contextFactory(userId, tracer, _))
  }

  def userAuthAndTrace(name: String): Directive1[UserLogContext] = {
    userAuth.flatMap(userId => withUserTrace(name, userId)(UserLogContext))
  }

  def adminAuthAndTrace(name: String): Directive1[AdminLogContext] = {
    adminAuth.flatMap(adminId => withUserTrace(name, adminId)(AdminLogContext))
  }

}
