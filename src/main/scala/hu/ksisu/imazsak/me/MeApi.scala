package hu.ksisu.imazsak.me

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.core.{AuthDirectives, JwtService}
import hu.ksisu.imazsak.me.MeApi._
import hu.ksisu.imazsak.me.MeService.{MeUserData, UpdateMeUserData}
import spray.json.{JsObject, RootJsonFormat}

import scala.concurrent.Future

class MeApi(implicit service: MeService[Future], val jwtService: JwtService[Future]) extends Api with AuthDirectives {

  def route(): Route = {
    path("me") {
      get {
        userAuthAndTrace("Me_Get") { implicit ctx =>
          onSuccess(service.getUserData()) { result =>
            complete(result)
          }
        }
      } ~ post {
        userAuthAndTrace("Me_Update") { implicit ctx =>
          entity(as[UpdateMeUserData]) { data =>
            onSuccess(service.updateUserData(data)) {
              complete(JsObject())
            }
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
