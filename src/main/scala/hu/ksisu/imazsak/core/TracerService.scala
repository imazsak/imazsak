package hu.ksisu.imazsak.core

import cats.MonadError
import hu.ksisu.imazsak.Initable
import hu.ksisu.imazsak.core.TracerService.TracerServiceConfig
import hu.ksisu.imazsak.core.Errors.WrongConfig
import io.jaegertracing.Configuration
import io.jaegertracing.Configuration.{ReporterConfiguration, SamplerConfiguration}
import io.opentracing.noop.NoopTracerFactory
import io.opentracing.util.GlobalTracer

class TracerService[F[_]: MonadError[*[_], Throwable]](implicit config: TracerServiceConfig) extends Initable[F] {
  import cats.syntax.applicative._
  import hu.ksisu.imazsak.util.ApplicativeErrorSyntax._

  override def init: F[Unit] = {
    config.client.toLowerCase match {
      case TracerService.OFF    => initNoop().pure
      case TracerService.JAEGER => initJaeger().pure
      case _                    => WrongConfig(s"Unsupported Tracer client: ${config.client}").raise
    }
  }

  private def initNoop(): Unit = {
    GlobalTracer.registerIfAbsent(NoopTracerFactory.create())
  }

  private def initJaeger(): Unit = {
    // todo from config
    val samplerConfig  = SamplerConfiguration.fromEnv().withType("const").withParam(1)
    val reporterConfig = ReporterConfiguration.fromEnv().withLogSpans(true)
    val config         = new Configuration("imazsak").withSampler(samplerConfig).withReporter(reporterConfig)

    GlobalTracer.registerIfAbsent(config.getTracer)
  }

}

object TracerService {
  val OFF: String    = "off"
  val JAEGER: String = "jaeger"
  case class TracerServiceConfig(client: String)
}
