package hu.ksisu.imazsak.notification

import hu.ksisu.imazsak.Errors._
import hu.ksisu.imazsak.notification.NotificationDao.NotificationMeta
import hu.ksisu.imazsak.notification.NotificationService.{NotificationListResponse, PushSubscribeRequest}
import hu.ksisu.imazsak.util.LoggerUtil.{LogContext, UserLogContext}
import spray.json.{JsValue, JsonWriter}

trait NotificationService[F[_]] {
  def pushSubscribe(data: PushSubscribeRequest)(implicit ctx: UserLogContext): Response[F, Unit]
  def pushUnsubscribe()(implicit ctx: UserLogContext): Response[F, Unit]
  def createNotification[T](notificationType: String, userId: String, message: T)(
      implicit ctx: LogContext,
      w: JsonWriter[T]
  ): Response[F, Unit]
  def listUserNotifications()(implicit ctx: UserLogContext): Response[F, Seq[NotificationListResponse]]
  def deleteUserNotifications(ids: Seq[String])(implicit ctx: UserLogContext): Response[F, Unit]
  def setUserRead(ids: Seq[String])(implicit ctx: UserLogContext): Response[F, Unit]
}

object NotificationService {
  case class NotificationListResponse(id: String, message: JsValue, createdAt: Long, meta: NotificationMeta)
  case class PushSubscribeRequest(endpoint: String, expirationTime: Option[Long], keys: Map[String, String])
}
