package hu.ksisu.imazsak.core.healthcheck

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.effect.IO
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.core.{AuthDirectives, JwtService}

class HealthCheckApi(implicit service: HealthCheckService[IO], val jwtService: JwtService[IO])
    extends Api
    with AuthDirectives {

  def route(): Route = {
    path("healthCheck") {
      get {
        withTrace("HealthCheck") { _ =>
          onSuccess(service.getStatus.unsafeToFuture()) { result =>
            complete(result)
          }
        }
      }
    }
  }
}
