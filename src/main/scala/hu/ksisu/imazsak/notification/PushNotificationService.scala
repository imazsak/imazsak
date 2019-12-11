package hu.ksisu.imazsak.notification

import hu.ksisu.imazsak.Errors.Response
import hu.ksisu.imazsak.Initable
import hu.ksisu.imazsak.notification.NotificationDao.CreateNotificationData
import hu.ksisu.imazsak.notification.PushNotificationService.PushSubscribeRequest
import hu.ksisu.imazsak.util.LoggerUtil.{LogContext, UserLogContext}

trait PushNotificationService[F[_]] extends Initable[F] {
  def getPublicKey()(implicit ctx: LogContext): Response[F, String]
  def addSubscription(data: PushSubscribeRequest)(implicit ctx: UserLogContext): Response[F, Unit]
  def removeSubscription(deviceId: String)(implicit ctx: LogContext): Response[F, Unit]
  def sendNotification(notificationId: String, data: CreateNotificationData)(
      implicit ctx: LogContext
  ): Response[F, Unit]
}

object PushNotificationService {
  case class PushNotificationConfig(publicKey: String, privateKey: String)
  case class PushSubscription(endpoint: String, expirationTime: Option[Long], keys: Map[String, String])
  case class PushSubscribeRequest(publicKey: String, deviceId: String, subscription: PushSubscription)
}
