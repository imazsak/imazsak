package hu.ksisu.imazsak.username

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directives.path
import hu.ksisu.imazsak.Errors._
import akka.http.scaladsl.server.Route
import cats.effect.IO
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.core.{AuthDirectives, JwtService}
import hu.ksisu.imazsak.username.UsernameSearchService.UsernameData
import hu.ksisu.imazsak.util.ApiHelper.Ids
import hu.ksisu.imazsak.util.LoggerUtil.Logger
import spray.json.DefaultJsonProtocol._
import spray.json._
import UsernameSearchApi._

class UsernameSearchApi(implicit service: UsernameSearchService[IO], val jwtService: JwtService[IO])
    extends Api
    with AuthDirectives {
  implicit val logger = new Logger("UsernameSearchApi")

  override def route(): Route = {
    path("username-search") {
      post {
        entity(as[Ids]) { data =>
          userAuthAndTrace("UsernameSearch_Search") { implicit ctx =>
            service.findUsernames(data.ids).toComplete
          }
        }
      }
    }
  }
}

object UsernameSearchApi {
  implicit val usernameDataFormat: RootJsonFormat[UsernameData] = jsonFormat2(UsernameData)
}
