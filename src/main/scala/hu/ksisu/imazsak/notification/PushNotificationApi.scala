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

class PushNotificationApi(implicit
    service: PushNotificationService[IO],
    val jwtService: JwtService[IO]
) extends Api
    with AuthDirectives {
  implicit val logger = new Logger("PushNotificationApi")

  def route(): Route = {
    pathPrefix("me" / "push-notification") {
      get {
        path("public-key") {
          withTrace("PushNotification_GetPublicKey") { implicit ctx =>
            service.getPublicKey().map(PublicKey).toComplete
          }
        }
      } ~ post {
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
      }
    }
  }

}

object PushNotificationApi {
  case class PublicKey(publicKey: String)
  case class DeviceId(deviceId: String)
  implicit val publicKeyFormat: RootJsonFormat[PublicKey]                       = jsonFormat1(PublicKey)
  implicit val deviceIdFormat: RootJsonFormat[DeviceId]                         = jsonFormat1(DeviceId)
  implicit val pushSubscriptionFormat: RootJsonFormat[PushSubscription]         = jsonFormat3(PushSubscription)
  implicit val pushSubscribeRequestFormat: RootJsonFormat[PushSubscribeRequest] = jsonFormat3(PushSubscribeRequest)
}
