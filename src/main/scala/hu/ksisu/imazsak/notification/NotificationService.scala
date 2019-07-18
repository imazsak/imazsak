package hu.ksisu.imazsak.notification

import hu.ksisu.imazsak.Errors._
import hu.ksisu.imazsak.notification.NotificationDao.NotificationListData
import hu.ksisu.imazsak.notification.NotificationService.CreateNotificationRequest
import hu.ksisu.imazsak.util.LoggerUtil.{LogContext, UserLogContext}

trait NotificationService[F[_]] {
  def createNotification(data: CreateNotificationRequest)(implicit ctx: LogContext): Response[F, Unit]
  def listUserNotifications()(implicit ctx: UserLogContext): Response[F, Seq[NotificationListData]]
  def deleteUserNotifications(ids: Seq[String])(implicit ctx: UserLogContext): Response[F, Unit]
  def setUserRead(ids: Seq[String])(implicit ctx: UserLogContext): Response[F, Unit]
}

object NotificationService {
  case class CreateNotificationMetaData(notificationType: Option[String])
  case class CreateNotificationRequest(userId: String, message: String, meta: CreateNotificationMetaData)
}
