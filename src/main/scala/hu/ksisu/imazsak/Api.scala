package hu.ksisu.imazsak

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import hu.ksisu.imazsak.core.healthcheck.HealthCheckApi
import hu.ksisu.imazsak.me.MeApi
import hu.ksisu.imazsak.util.LoggerUtil.LogContext
import hu.ksisu.imazsak.util.TracingDirectives._
import io.opentracing.Tracer
import io.opentracing.util.GlobalTracer
import org.slf4j.Logger

import scala.concurrent.Future

trait Api {
  protected val tracer: Tracer                                  = GlobalTracer.get()
  protected def withTrace(name: String): Directive1[LogContext] = trace(tracer, name).map(new LogContext(tracer, _))

  def route(): Route
}

object Api {
  val emptyRoute = Route(_.reject())

  def createRoute(api: Seq[Api]): Route = {
    api
      .map(_.route())
      .fold(Api.emptyRoute)(_ ~ _)
  }

  def createApi(services: Services[Future])(implicit logger: Logger): Route = {
    import services._

    val modules = configService.getEnabledModules
    logger.info(s"Load api for modules: ${modules.mkString(",")}")

    val api = modules.collect {
      case "health" => new HealthCheckApi()
    } ++ Seq(
      new MeApi()
    )

    cors() {
      createRoute(api)
    }
  }
}
