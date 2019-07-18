package hu.ksisu.imazsak.user

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.Errors._
import hu.ksisu.imazsak.core.{AuthDirectives, JwtService}
import hu.ksisu.imazsak.user.MeApi._
import hu.ksisu.imazsak.user.MeService.{MeUserData, UpdateMeUserData}
import hu.ksisu.imazsak.util.LoggerUtil.Logger
import spray.json.RootJsonFormat

import scala.concurrent.Future

class MeApi(implicit service: MeService[Future], val jwtService: JwtService[Future]) extends Api with AuthDirectives {
  implicit val logger = new Logger("MeApi")

  def route(): Route = {
    path("me") {
      get {
        userAuthAndTrace("Me_Get") { implicit ctx =>
          service.getUserData().toComplete
        }
      } ~ post {
        userAuthAndTrace("Me_Update") { implicit ctx =>
          entity(as[UpdateMeUserData]) { data =>
            service.updateUserData(data).toComplete
          }
        }
      }
    }
  }
}

object MeApi {
  import spray.json.DefaultJsonProtocol._
  implicit val meUserDataFormat: RootJsonFormat[MeUserData]             = jsonFormat1(MeUserData)
  implicit val updateMeUserDataFormat: RootJsonFormat[UpdateMeUserData] = jsonFormat1(UpdateMeUserData)

}
