package hu.ksisu.imazsak.prayer

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.effect.IO
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.Errors._
import hu.ksisu.imazsak.core.{AuthDirectives, JwtService}
import hu.ksisu.imazsak.prayer.PrayerApi._
import hu.ksisu.imazsak.prayer.PrayerDao.PrayerUpdateData
import hu.ksisu.imazsak.prayer.PrayerService.{
  CreatePrayerRequest,
  Next10PrayerListData,
  PrayerCloseRequest,
  PrayerDetailsResponse,
  PrayerUpdateRequest
}
import hu.ksisu.imazsak.prayer.PrayerServiceImpl._
import hu.ksisu.imazsak.util.ApiHelper._
import hu.ksisu.imazsak.util.LoggerUtil.Logger
import spray.json.DefaultJsonProtocol._
import spray.json.{JsObject, RootJsonFormat}

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
    } ~ get {
      path("prayers" / Segment) { prayerId =>
        userAuthAndTrace("Prayer_Details") { implicit ctx =>
          service.getPrayerDetails(prayerId).toComplete
        }
      }
    } ~ post {
      path("groups" / Segment / "prayers" / Segment / "pray") { (groupId, prayerId) =>
        entity(as[JsObject]) { _ =>
          userAuthAndTrace("Prayer_Pray") { implicit ctx =>
            service.pray(groupId, prayerId).toComplete
          }
        }
      }
    } ~ post {
      path("prayers" / "next-10") {
        entity(as[Ids]) { data =>
          userAuthAndTrace("Prayer_Next10") { implicit ctx =>
            service.next10(data.ids).toComplete
          }
        }
      } ~ post {
        path("prayers" / "close") {
          entity(as[PrayerCloseRequest]) { data =>
            userAuthAndTrace("Prayer_Close") { implicit ctx =>
              service.close(data).toComplete
            }
          }
        }
      } ~ post {
        path("prayers" / "update") {
          entity(as[PrayerUpdateRequest]) { data =>
            userAuthAndTrace("Prayer_Update") { implicit ctx =>
              service.addUpdateToPrayer(data).toComplete
            }
          }
        }
      }
    }
  }
}

object PrayerApi {
  implicit val createPrayerRequestFormat: RootJsonFormat[CreatePrayerRequest]     = jsonFormat2(CreatePrayerRequest)
  implicit val next10PrayerListDataFormat: RootJsonFormat[Next10PrayerListData]   = jsonFormat4(Next10PrayerListData)
  implicit val prayerCloseRequestFormat: RootJsonFormat[PrayerCloseRequest]       = jsonFormat2(PrayerCloseRequest)
  implicit val prayerUpdateDataFormat: RootJsonFormat[PrayerUpdateData]           = jsonFormat2(PrayerUpdateData)
  implicit val prayerDetailsResponseFormat: RootJsonFormat[PrayerDetailsResponse] = jsonFormat5(PrayerDetailsResponse)
  implicit val prayerUpdateRequestFormat: RootJsonFormat[PrayerUpdateRequest]     = jsonFormat2(PrayerUpdateRequest)
}
