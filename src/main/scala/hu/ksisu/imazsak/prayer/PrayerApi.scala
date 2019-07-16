package hu.ksisu.imazsak.prayer

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.core.{AuthDirectives, JwtService}
import hu.ksisu.imazsak.prayer.PrayerService.CreatePrayerRequest
import spray.json.{JsObject, RootJsonFormat}
import hu.ksisu.imazsak.prayer.PrayerApi._

import scala.concurrent.Future

class PrayerApi(implicit service: PrayerService[Future], val jwtService: JwtService[Future])
    extends Api
    with AuthDirectives {

  def route(): Route = {
    path("me" / "prayer") {
      post {
        userAuthAndTrace("Prayer_Create") { implicit ctx =>
          entity(as[CreatePrayerRequest]) { data =>
            onSuccess(service.createPrayer(data)) {
              complete(JsObject())
            }
          }
        }
      }
    }
  }
}

object PrayerApi {
  import spray.json.DefaultJsonProtocol._
  implicit val createPrayerRequestFormat: RootJsonFormat[CreatePrayerRequest] = jsonFormat2(CreatePrayerRequest)

}
