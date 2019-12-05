package hu.ksisu.imazsak.notification

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.effect.IO
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.Errors._
import hu.ksisu.imazsak.core.{AuthDirectives, JwtService}
import hu.ksisu.imazsak.notification.PushNotificationApi._
import hu.ksisu.imazsak.notification.PushNotificationService.{PushSubscribeRequest, PushSubscription}
import hu.ksisu.imazsak.util.LoggerUtil.Logger
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

class PushNotificationApi(
    implicit service: PushNotificationService[IO],
    val jwtService: JwtService[IO]
) extends Api
    with AuthDirectives {
  implicit val logger = new Logger("PushNotificationApi")

  def route(): Route = {
    pathPrefix("me" / "push-notification") {
      post {
        path("subscribe") {
          userAuthAndTrace("PushNotification_Subscribe") { implicit ctx =>
            entity(as[PushSubscribeRequest]) { data =>
              service.addSubscription(data).toComplete
            }
          }
        }
      } ~ {
        path("unsubscribe") {
          withTrace("PushNotification_Unsubscribe") { implicit ctx =>
            entity(as[DeviceId]) { data =>
              service.removeSubscription(data.deviceId).toComplete
            }
          }
        }
      } ~ {
        path("test") {
          userAuthAndTrace("PushNotification_Test") { implicit ctx =>
            service.sendNotification(ctx.userId, "Hello! :)").toComplete
          }
        }
      }
    }
  }

}

object PushNotificationApi {
  case class DeviceId(deviceId: String)
  implicit val deviceIdFormat: RootJsonFormat[DeviceId]                         = jsonFormat1(DeviceId)
  implicit val pushSubscriptionFormat: RootJsonFormat[PushSubscription]         = jsonFormat3(PushSubscription)
  implicit val pushSubscribeRequestFormat: RootJsonFormat[PushSubscribeRequest] = jsonFormat2(PushSubscribeRequest)
}
