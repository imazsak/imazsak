package hu.ksisu.imazsak.notification

import hu.ksisu.imazsak.Errors._
import hu.ksisu.imazsak.Initable
import hu.ksisu.imazsak.notification.NotificationDao.NotificationMeta
import hu.ksisu.imazsak.notification.NotificationService.{NotificationInfoResponse, NotificationListResponse}
import hu.ksisu.imazsak.util.LoggerUtil.{LogContext, UserLogContext}
import spray.json._

trait NotificationService[F[_]] extends Initable[F] {
  def createNotification[T](notificationType: String, userId: String, message: T)(
      implicit ctx: LogContext,
      w: JsonWriter[T]
  ): Response[F, Unit]
  def listUserNotifications()(implicit ctx: UserLogContext): Response[F, Seq[NotificationListResponse]]
  def userNotificationsInfo()(implicit ctx: UserLogContext): Response[F, NotificationInfoResponse]
  def deleteUserNotifications(ids: Seq[String])(implicit ctx: UserLogContext): Response[F, Unit]
  def setUserRead(ids: Seq[String])(implicit ctx: UserLogContext): Response[F, Unit]
}

object NotificationService {
  import spray.json.DefaultJsonProtocol._
  case class NotificationListResponse(id: String, message: JsValue, createdAt: Long, meta: NotificationMeta)
  case class NotificationInfoResponse(c: String)
  implicit val notificationInfoResponseFormat: RootJsonFormat[NotificationInfoResponse] = jsonFormat1(
    NotificationInfoResponse
  )
}
