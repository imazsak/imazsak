package hu.ksisu.imazsak.core.healthcheck

import hu.ksisu.imazsak.core.healthcheck.HealthCheckService.HealthCheckResult

trait HealthCheckService[F[_]] {
  def getStatus: F[HealthCheckResult]
}

object HealthCheckService {
  import spray.json.DefaultJsonProtocol._
  final case class HealthCheckResult(
      success: Boolean,
      version: String,
      redis: Boolean,
      database: Boolean,
      buildAtString: String,
      buildAtMillis: Long,
      commitHash: Option[String]
  )
  implicit val healthCheckResultFormat = jsonFormat7(HealthCheckResult)
}
