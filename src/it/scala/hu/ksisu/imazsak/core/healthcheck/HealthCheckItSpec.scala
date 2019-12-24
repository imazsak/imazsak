package hu.ksisu.imazsak.core.healthcheck

import akka.http.scaladsl.model.StatusCodes
import hu.ksisu.imazsak.{BuildInfo, RouteTestBase}

class HealthCheckItSpec extends RouteTestBase {
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import hu.ksisu.imazsak.core.healthcheck.HealthCheckService._

  "GET /healthCheck" should {
    "return OK" in new BaseTestScope {
      Get("/healthCheck") ~> route ~> check {
        status shouldEqual StatusCodes.OK
      }
    }
    "return with data" in new BaseTestScope {
      Get("/healthCheck") ~> route ~> check {
        responseAs[HealthCheckResult] shouldEqual HealthCheckResult(
          true,
          BuildInfo.version,
          Map(
            "redis"    -> true,
            "database" -> true
          ),
          BuildInfo.builtAtString,
          BuildInfo.builtAtMillis,
          BuildInfo.commitHash
        )
      }
    }
  }
}
