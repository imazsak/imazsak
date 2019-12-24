package hu.ksisu.imazsak.core.healthcheck

import hu.ksisu.imazsak.core.healthcheck.HealthCheckService.HealthCheckResult

trait HealthCheckService[F[_]] {
  def addModule(name: String, check: () => F[Boolean]): Unit
  def getStatus: F[HealthCheckResult]
}

object HealthCheckService {
  import spray.json.DefaultJsonProtocol._
  final case class HealthCheckResult(
      success: Boolean,
      version: String,
      modules: Map[String, Boolean],
      buildAtString: String,
      buildAtMillis: Long,
      commitHash: Option[String]
  )
  implicit val healthCheckResultFormat = jsonFormat6(HealthCheckResult)
}
