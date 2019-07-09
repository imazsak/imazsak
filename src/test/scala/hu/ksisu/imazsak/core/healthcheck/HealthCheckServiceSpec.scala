package hu.ksisu.imazsak.core.healthcheck

import hu.ksisu.imazsak.core.config.ServerConfig
import hu.ksisu.imazsak.core.dao.MongoDatabaseService
import hu.ksisu.imazsak.core.healthcheck.HealthCheckService.HealthCheckResult
import hu.ksisu.imazsak.{BuildInfo, TestBase}
import org.mockito.Mockito.when

import scala.util.{Failure, Success, Try}

class HealthCheckServiceSpec extends TestBase {

  trait TestScope {
    import cats.instances.try_._
    implicit val configService: ServerConfig[Try]               = mock[ServerConfig[Try]]
    implicit val databaseServiceMock: MongoDatabaseService[Try] = mock[MongoDatabaseService[Try]]
    val service                                                 = new HealthCheckServiceImpl[Try]()
  }

  "#getStatus" when {
    "ok" in new TestScope {
      when(databaseServiceMock.checkStatus()).thenReturn(Success(true))

      service.getStatus.get shouldEqual HealthCheckResult(
        true,
        BuildInfo.version,
        true,
        BuildInfo.builtAtString,
        BuildInfo.builtAtMillis,
        BuildInfo.commitHash
      )
    }
    "db failed" in new TestScope {
      when(databaseServiceMock.checkStatus()).thenReturn(Success(false))

      service.getStatus.get shouldEqual HealthCheckResult(
        false,
        BuildInfo.version,
        false,
        BuildInfo.builtAtString,
        BuildInfo.builtAtMillis,
        BuildInfo.commitHash
      )
    }
    "db check failed" in new TestScope {
      when(databaseServiceMock.checkStatus()).thenReturn(Failure(new Exception))

      service.getStatus.get shouldEqual HealthCheckResult(
        false,
        BuildInfo.version,
        false,
        BuildInfo.builtAtString,
        BuildInfo.builtAtMillis,
        BuildInfo.commitHash
      )
    }
  }
}
