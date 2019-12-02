package hu.ksisu.imazsak.notification

import hu.ksisu.imazsak.Errors.Response
import hu.ksisu.imazsak.Initable
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

trait PushNotificationService[F[_]] extends Initable[F] {
  def sendPushNotification(userId: String, message: String)(implicit ctx: UserLogContext): Response[F, Unit]
}

object PushNotificationService {
  case class PushNotificationConfig(publicKey: String, privateKey: String)
}
