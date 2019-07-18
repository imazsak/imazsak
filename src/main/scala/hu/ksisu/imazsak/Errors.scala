package hu.ksisu.imazsak

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.data.EitherT
import hu.ksisu.imazsak.util.LoggerUtil.{LogContext, Logger}
import spray.json._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Errors {
  type Response[F[_], T] = EitherT[F, Throwable, T]

  implicit class ResponseWrapper[T](val response: Response[Future, T]) {
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

      onComplete(response.value) {
        case Success(Right(res))  => complete(res)
        case Success(Left(error)) => errorHandler(error)
        case Failure(error) =>
          logger.error("Unhandled error!", error)
          complete(StatusCodes.InternalServerError)
      }
    }
  }

  type ErrorHandler = PartialFunction[Throwable, Route]

  private def defaultHandler(implicit logger: Logger, ctx: LogContext): ErrorHandler = {
    case ex: NoSuchElementException   => complete((StatusCodes.NotFound, ErrorResponse.fromEx(ex)))
    case ex: IllegalAccessError       => complete((StatusCodes.Forbidden, ErrorResponse.fromEx(ex)))
    case ex: IllegalArgumentException => complete((StatusCodes.BadRequest, ErrorResponse.fromEx(ex)))
    case ex =>
      logger.warn("Unknown error!", ex)
      complete(StatusCodes.InternalServerError)
  }

  case class ErrorResponse(error: String)
  object ErrorResponse {
    def fromEx(ex: Throwable): ErrorResponse = {
      ErrorResponse(ex.getMessage)
    }
  }
  import spray.json.DefaultJsonProtocol._
  implicit val errorResponseFormat: RootJsonFormat[ErrorResponse] = jsonFormat1(ErrorResponse.apply)
  implicit val unitWriter: RootJsonWriter[Unit]                   = (_: Unit) => JsObject()
}
