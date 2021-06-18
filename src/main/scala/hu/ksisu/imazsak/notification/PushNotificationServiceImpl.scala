package hu.ksisu.imazsak.notification

import java.nio.charset.StandardCharsets
import java.security.Security

import cats.Applicative
import cats.data.EitherT
import cats.effect.IO
import hu.ksisu.imazsak.Errors.{Response, VapidPublicKeyChanged}
import hu.ksisu.imazsak.notification.NotificationDao.{CreateNotificationData, NotificationMeta}
import hu.ksisu.imazsak.notification.PushNotificationService.{PushNotificationConfig, PushSubscribeRequest}
import hu.ksisu.imazsak.prayer.PrayerService._
import hu.ksisu.imazsak.user.UserDao
import hu.ksisu.imazsak.user.UserDao.UserPushSubscriptionData
import hu.ksisu.imazsak.util.DateTimeUtil
import hu.ksisu.imazsak.util.LoggerUtil.{LogContext, Logger, UserLogContext}
import nl.martijndwars.webpush._
import org.bouncycastle.jce.provider.BouncyCastleProvider
import spray.json._

import scala.io.Source

class PushNotificationServiceImpl(implicit
    config: PushNotificationConfig,
    userDao: UserDao[IO],
    ev: Applicative[IO],
    date: DateTimeUtil
) extends PushNotificationService[IO] {
  private implicit val logger = new Logger("PushNotificationService")

  private lazy val clientF = IO {
    val publicKey  = Utils.loadPublicKey(config.publicKey)
    val privateKey = Utils.loadPrivateKey(config.privateKey)
    val c          = new PushService()
    c.setPrivateKey(privateKey)
    c.setPublicKey(publicKey)
    c
  }

  override def init: IO[Unit] = {
    Security.addProvider(new BouncyCastleProvider)
    clientF.map(_ => ())
  }

  override def getPublicKey()(implicit ctx: LogContext): Response[IO, String] = {
    EitherT.rightT(config.publicKey)
  }

  override def addSubscription(data: PushSubscribeRequest)(implicit ctx: UserLogContext): Response[IO, Unit] = {
    if (data.publicKey != config.publicKey) {
      EitherT.leftT(VapidPublicKeyChanged("Vapid public key changed!"))
    } else {
      val daoData = UserPushSubscriptionData(
        data.subscription.endpoint,
        data.subscription.expirationTime,
        data.subscription.keys
      )
      EitherT.right(userDao.addPushSubscription(ctx.userId, data.deviceId, daoData))
    }
  }

  override def removeSubscription(deviceId: String)(implicit ctx: LogContext): Response[IO, Unit] = {
    EitherT.right(userDao.removePushSubscriptionByDeviceId(deviceId))
  }

  def sendNotification(notificationId: String, data: CreateNotificationData)(implicit
      ctx: LogContext
  ): Response[IO, Unit] = {
    import cats.instances.list._
    import cats.syntax.traverse._

    val payload = createPushMessage(notificationId, data)
    EitherT
      .right(userDao.findPushSubscriptionsByUserId(data.userId))
      .flatMap(_.toList.traverse[Response[IO, *], Unit](sub => _sendPushNotification(sub, payload, 86400)))
      .map(_ => {})
  }

  private def createPushMessage(notificationId: String, data: CreateNotificationData): String = {
    JsObject(
      "notification" -> JsObject(
        "title"   -> JsString("Imazsák"),
        "body"    -> JsString(renderMessage(data)),
        "icon"    -> JsString("assets/icons/icon-72x72.png"),
        "vibrate" -> JsArray(JsNumber(100), JsNumber(50), JsNumber(100)),
        "data" -> JsObject(
          "dateOfArrival"    -> JsNumber(date.getCurrentTimeMillis),
          "primaryKey"       -> JsNumber(1),
          "notificationId"   -> JsString(notificationId),
          "notificationType" -> JsString(data.meta.notificationType.getOrElse(""))
        )
      )
    ).compactPrint
  }

  private def renderMessage(data: CreateNotificationData): String = {
    data.meta.notificationType match {
      case Some(NotificationMeta.PRAYER_CREATED) =>
        val msgData = data.message.parseJson.convertTo[PrayerCreatedNotificationData]
        msgData.userName.fold {
          "Új imakérés!"
        } { name =>
          s"$name új imát kért!"
        }
      case Some(NotificationMeta.PRAYER_CLOSE_FEEDBACK) =>
        val msgData = data.message.parseJson.convertTo[PrayerCloseFeedbackNotificationData]
        msgData.userName.map(n => s"$n ").getOrElse("") + msgData.feedback
      case _ =>
        "Új értesítés!"
    }
  }

  private def _sendPushNotification(subscriptionData: UserPushSubscriptionData, payload: String, ttl: Int)(implicit
      ctx: LogContext
  ): Response[IO, Unit] = {
    val result = clientF.map { client =>
      val noti = new Notification(
        subscriptionData.endpoint,
        Utils.loadPublicKey(subscriptionData.keys("p256dh")),
        Base64Encoder.decode(subscriptionData.keys("auth")),
        payload.getBytes(StandardCharsets.UTF_8),
        ttl
      )
      val response = client.send(noti)
      val status   = response.getStatusLine.getStatusCode
      if (status < 200 || 300 <= status) {
        val body = Source
          .fromInputStream(
            response.getEntity.getContent
          )
          .getLines()
          .mkString("\n")
        logger.warn(s"PUSH FAILED! \n $payload \n $status - $body");
      }
    }
    EitherT.right(result)
  }
}
