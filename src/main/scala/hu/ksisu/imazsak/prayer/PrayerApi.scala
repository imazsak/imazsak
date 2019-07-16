package hu.ksisu.imazsak.prayer

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.Errors._
import hu.ksisu.imazsak.core.dao.PrayerDao.{GroupPrayerListData, MinePrayerListData}
import hu.ksisu.imazsak.core.{AuthDirectives, JwtService}
import hu.ksisu.imazsak.prayer.PrayerApi._
import hu.ksisu.imazsak.prayer.PrayerService.CreatePrayerRequest
import hu.ksisu.imazsak.util.LoggerUtil.Logger
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.Future

class PrayerApi(implicit service: PrayerService[Future], val jwtService: JwtService[Future])
    extends Api
    with AuthDirectives {
  implicit val logger = new Logger("PrayerApi")

  def route(): Route = {
    path("prayers") {
      post {
        userAuthAndTrace("Prayer_Create") { implicit ctx =>
          entity(as[CreatePrayerRequest]) { data =>
            service.createPrayer(data).toComplete
          }
        }
      } ~ get {
        userAuthAndTrace("Prayer_ListMine") { implicit ctx =>
          service.listMyPrayers().toComplete
        }
      }
    } ~ {
      path("groups" / Segment / "prayers") { groupId =>
        userAuthAndTrace("Prayer_ListGroup") { implicit ctx =>
          service.listGroupPrayers(groupId).toComplete
        }
      }
    }
  }
}

object PrayerApi {
  implicit val createPrayerRequestFormat: RootJsonFormat[CreatePrayerRequest] = jsonFormat2(CreatePrayerRequest)
  implicit val minePrayerListDataFormat: RootJsonFormat[MinePrayerListData]   = jsonFormat3(MinePrayerListData)
  implicit val groupPrayerListDataFormat: RootJsonFormat[GroupPrayerListData] = jsonFormat3(GroupPrayerListData)
}
