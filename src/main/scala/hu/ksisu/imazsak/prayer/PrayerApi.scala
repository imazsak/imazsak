package hu.ksisu.imazsak.prayer

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.effect.IO
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.Errors._
import hu.ksisu.imazsak.core.{AuthDirectives, JwtService}
import hu.ksisu.imazsak.prayer.PrayerApi._
import hu.ksisu.imazsak.prayer.PrayerDao.{GroupPrayerListData, MyPrayerListData}
import hu.ksisu.imazsak.prayer.PrayerService.{CreatePrayerRequest, Next10PrayerListData}
import hu.ksisu.imazsak.util.ApiHelper._
import hu.ksisu.imazsak.util.LoggerUtil.Logger
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

class PrayerApi(implicit service: PrayerService[IO], val jwtService: JwtService[IO]) extends Api with AuthDirectives {
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
    } ~ get {
      path("groups" / Segment / "prayers") { groupId =>
        userAuthAndTrace("Prayer_ListGroup") { implicit ctx =>
          service.listGroupPrayers(groupId).toComplete
        }
      }
    } ~ post {
      path("groups" / Segment / "prayers" / Segment / "pray") { (groupId, prayerId) =>
        userAuthAndTrace("Prayer_Pray") { implicit ctx =>
          service.pray(groupId, prayerId).toComplete
        }
      }
    } ~ post {
      path("prayers" / "next-10") {
        entity(as[Ids]) { data =>
          userAuthAndTrace("Prayer_Next10") { implicit ctx =>
            service.next10(data.ids).toComplete
          }
        }
      }
    }
  }
}

object PrayerApi {
  implicit val createPrayerRequestFormat: RootJsonFormat[CreatePrayerRequest]   = jsonFormat2(CreatePrayerRequest)
  implicit val minePrayerListDataFormat: RootJsonFormat[MyPrayerListData]       = jsonFormat4(MyPrayerListData)
  implicit val groupPrayerListDataFormat: RootJsonFormat[GroupPrayerListData]   = jsonFormat3(GroupPrayerListData)
  implicit val next10PrayerListDataFormat: RootJsonFormat[Next10PrayerListData] = jsonFormat4(Next10PrayerListData)
}
