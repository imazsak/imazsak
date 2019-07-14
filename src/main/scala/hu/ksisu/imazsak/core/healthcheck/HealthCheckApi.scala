package hu.ksisu.imazsak.core.healthcheck

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.core.{AuthDirectives, JwtService}

import scala.concurrent.Future

class HealthCheckApi(implicit service: HealthCheckService[Future], val jwtService: JwtService[Future])
    extends Api
    with AuthDirectives {

  def route(): Route = {
    path("healthCheck") {
      get {
        withTrace("HealthCheck") { _ =>
          onSuccess(service.getStatus) { result =>
            complete(result)
          }
        }
      }
    } ~ path("test") {
      userAuth { id =>
        complete(s"hello $id")
      }
    }
  }
}
