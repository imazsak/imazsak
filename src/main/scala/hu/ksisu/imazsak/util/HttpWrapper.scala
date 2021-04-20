package hu.ksisu.imazsak.util

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import cats.effect.IO
import hu.ksisu.imazsak.util.LoggerUtil.LogContext
import spray.json.RootJsonReader

import scala.reflect.ClassTag

trait HttpWrapper[F[_]] {
  def singleRequest(httpRequest: HttpRequest)(implicit ctx: LogContext): F[HttpResponse]
  def unmarshalEntityTo[T: ClassTag: RootJsonReader](resp: HttpResponse): F[T]
  def unmarshalEntityToString(resp: HttpResponse): F[String]
}

class AkkaHttpWrapper(implicit actorSystem: ActorSystem, materializer: Materializer, cs: ContextShift[IO])
    extends HttpWrapper[IO] {

  override def singleRequest(httpRequest: HttpRequest)(implicit ctx: LogContext): IO[HttpResponse] = {
    val requestWithTrace = httpRequest.copy(headers = httpRequest.headers ++ ctx.getInjectHeaders)
    IO.fromFuture(IO(Http().singleRequest(requestWithTrace)))
  }

  override def unmarshalEntityTo[T: ClassTag: RootJsonReader](resp: HttpResponse): IO[T] = {
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
    IO.fromFuture(IO(Unmarshal(resp.entity).to[T]))
  }

  override def unmarshalEntityToString(resp: HttpResponse): IO[String] = {
    IO.fromFuture(IO(Unmarshal(resp.entity).to[String]))
  }
}
