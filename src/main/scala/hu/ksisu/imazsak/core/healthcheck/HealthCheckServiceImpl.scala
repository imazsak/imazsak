package hu.ksisu.imazsak.core.healthcheck

import cats.MonadError
import hu.ksisu.imazsak.BuildInfo
import hu.ksisu.imazsak.core.CacheService
import hu.ksisu.imazsak.core.dao.MongoDatabaseService
import hu.ksisu.imazsak.core.healthcheck.HealthCheckService.HealthCheckResult

class HealthCheckServiceImpl[F[_]](implicit
    F: MonadError[F, Throwable],
    databaseService: MongoDatabaseService[F],
    redisService: CacheService[F]
) extends HealthCheckService[F] {
  import cats.syntax.applicativeError._
  import cats.syntax.functor._
  import cats.syntax.traverse._

  val services = new scala.collection.concurrent.TrieMap[String, () => F[Boolean]]()
  addModule("database", () => databaseService.checkStatus())
  addModule("redis", () => redisService.checkStatus())

  def getStatus: F[HealthCheckResult] = {
    for {
      serviceResults <- services.toList.traverse {
        case (name, check) =>
          check().recover { case _ => false }.map(result => (name, result))
      }
    } yield {
      val success = serviceResults.forall(_._2)
      HealthCheckResult(
        success,
        BuildInfo.version,
        serviceResults.toMap,
        BuildInfo.builtAtString,
        BuildInfo.builtAtMillis,
        BuildInfo.commitHash
      )
    }
  }

  override def addModule(name: String, check: () => F[Boolean]): Unit = {
    services += (name -> check)
  }
}
