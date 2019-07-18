package hu.ksisu.imazsak.notification

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.Errors._
import hu.ksisu.imazsak.core.{AuthDirectives, JwtService}
import hu.ksisu.imazsak.notification.NotificationApi._
import hu.ksisu.imazsak.notification.NotificationDao.{NotificationListData, NotificationMeta}
import hu.ksisu.imazsak.util.LoggerUtil.Logger
import spray.json.DefaultJsonProtocol._
import spray.json._
import hu.ksisu.imazsak.util.ApiHelper._

import scala.concurrent.Future

class NotificationApi(implicit service: NotificationService[Future], val jwtService: JwtService[Future])
    extends Api
    with AuthDirectives {
  implicit val logger = new Logger("NotificationApi")

  def route(): Route = {
    pathPrefix("me" / "notifications") {
      get {
        userAuthAndTrace("Notifications_List") { implicit ctx =>
          service.listUserNotifications().toComplete
        }
      } ~ post {
        path("read") {
          userAuthAndTrace("Notifications_Read") { implicit ctx =>
            entity(as[Ids]) { data =>
              service.setUserRead(data.ids).toComplete
            }
          }
        } ~
          path("delete") {
            userAuthAndTrace("Notifications_Delete") { implicit ctx =>
              entity(as[Ids]) { data =>
                service.deleteUserNotifications(data.ids).toComplete
              }
            }
          }
      }
    }
  }

}

object NotificationApi {
  implicit val notificationMetaFormat: RootJsonFormat[NotificationMeta]         = jsonFormat2(NotificationMeta)
  implicit val notificationListDataFormat: RootJsonFormat[NotificationListData] = jsonFormat4(NotificationListData)
}
