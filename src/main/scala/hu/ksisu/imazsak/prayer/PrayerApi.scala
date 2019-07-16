package hu.ksisu.imazsak.prayer

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.Errors._
import hu.ksisu.imazsak.core.{AuthDirectives, JwtService}
import hu.ksisu.imazsak.prayer.PrayerApi._
import hu.ksisu.imazsak.prayer.PrayerService.CreatePrayerRequest
import hu.ksisu.imazsak.util.LoggerUtil.Logger
import spray.json.RootJsonFormat

import scala.concurrent.Future

class PrayerApi(implicit service: PrayerService[Future], val jwtService: JwtService[Future])
    extends Api
    with AuthDirectives {
  implicit val logger = new Logger("PrayerApi")

  def route(): Route = {
    path("me" / "prayer") {
      post {
        userAuthAndTrace("Prayer_Create") { implicit ctx =>
          entity(as[CreatePrayerRequest]) { data =>
            service.createPrayer(data).toComplete
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
