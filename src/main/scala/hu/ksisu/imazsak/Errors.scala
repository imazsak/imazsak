package hu.ksisu.imazsak

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.data.EitherT
import cats.effect.IO
import hu.ksisu.imazsak.util.LoggerUtil.{LogContext, Logger}
import spray.json._

import scala.util.{Failure, Success}

object Errors {
  trait AppError
  case class IllegalArgumentError(message: String) extends AppError
  case class AccessDeniedError(message: String)    extends AppError
  case class NotFoundError(message: String)        extends AppError

  type Response[F[_], T] = EitherT[F, AppError, T]

  implicit class ResponseWrapper[T](val response: Response[IO, T]) {
    def toComplete(implicit w: RootJsonWriter[T], logger: Logger, ctx: LogContext): Route = {
      toComplete(None)
    }
    def toComplete(handler: ErrorHandler)(implicit w: RootJsonWriter[T], logger: Logger, ctx: LogContext): Route = {
      toComplete(Option(handler))
    }

    private def toComplete(
        mbHandler: Option[ErrorHandler]
    )(implicit w: RootJsonWriter[T], logger: Logger, ctx: LogContext) = {
      val errorHandler = mbHandler.map(_.orElse(defaultHandler)).getOrElse(defaultHandler)

      onComplete(response.value.unsafeToFuture()) {
        case Success(Right(res))  => complete(res)
        case Success(Left(error)) => errorHandler(error)
        case Failure(error) =>
          logger.error("Unhandled error!", error)
          complete(StatusCodes.InternalServerError)
      }
    }
  }

  type ErrorHandler = PartialFunction[AppError, Route]

  private def defaultHandler: ErrorHandler = {
    case NotFoundError(msg)        => complete((StatusCodes.NotFound, ErrorResponse(msg)))
    case AccessDeniedError(msg)    => complete((StatusCodes.Forbidden, ErrorResponse(msg)))
    case IllegalArgumentError(msg) => complete((StatusCodes.BadRequest, ErrorResponse(msg)))
  }

  case class ErrorResponse(error: String)
  import spray.json.DefaultJsonProtocol._
  implicit val errorResponseFormat: RootJsonFormat[ErrorResponse] = jsonFormat1(ErrorResponse.apply)
  implicit val unitWriter: RootJsonWriter[Unit]                   = (_: Unit) => JsObject()
}
