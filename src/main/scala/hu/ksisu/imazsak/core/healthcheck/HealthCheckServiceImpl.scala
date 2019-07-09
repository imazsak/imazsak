package hu.ksisu.imazsak.core.healthcheck

import cats.MonadError
import hu.ksisu.imazsak.BuildInfo
import hu.ksisu.imazsak.core.dao.MongoDatabaseService
import hu.ksisu.imazsak.core.healthcheck.HealthCheckService.HealthCheckResult

class HealthCheckServiceImpl[F[_]](
    implicit F: MonadError[F, Throwable],
    databaseService: MongoDatabaseService[F]
) extends HealthCheckService[F] {
  import cats.syntax.applicativeError._
  import cats.syntax.functor._

  def getStatus: F[HealthCheckResult] = {
    for {
      dbStatus <- databaseService.checkStatus().recover { case _ => false }
    } yield {
      val success = dbStatus
      HealthCheckResult(
        success,
        BuildInfo.version,
        dbStatus,
        BuildInfo.builtAtString,
        BuildInfo.builtAtMillis,
        BuildInfo.commitHash
      )
    }
  }
}
