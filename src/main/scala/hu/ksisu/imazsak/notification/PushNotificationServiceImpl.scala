package hu.ksisu.imazsak.notification

import java.nio.charset.StandardCharsets
import java.security.Security

import cats.Applicative
import cats.data.EitherT
import cats.effect.IO
import hu.ksisu.imazsak.Errors.Response
import hu.ksisu.imazsak.notification.PushNotificationService.{PushNotificationConfig, PushSubscribeRequest}
import hu.ksisu.imazsak.user.UserDao
import hu.ksisu.imazsak.user.UserDao.UserPushSubscriptionData
import hu.ksisu.imazsak.util.LoggerUtil.{LogContext, Logger, UserLogContext}
import nl.martijndwars.webpush._
import org.bouncycastle.jce.provider.BouncyCastleProvider

import scala.io.Source

class PushNotificationServiceImpl(implicit config: PushNotificationConfig, userDao: UserDao[IO], ev: Applicative[IO])
    extends PushNotificationService[IO] {
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

  override def addSubscription(data: PushSubscribeRequest)(implicit ctx: UserLogContext): Response[IO, Unit] = {
    val daoData = UserPushSubscriptionData(
      data.subscription.endpoint,
      data.subscription.expirationTime,
      data.subscription.keys
    )
    EitherT.right(userDao.addPushSubscription(ctx.userId, data.deviceId, daoData))
  }

  override def removeSubscription(deviceId: String)(implicit ctx: LogContext): Response[IO, Unit] = {
    EitherT.right(userDao.removePushSubscriptionByDeviceId(deviceId))
  }

  def sendNotification(userId: String, message: String)(implicit ctx: UserLogContext): Response[IO, Unit] = {
    val payload = s"""{
                     |    "notification": {
                     |        "title": "Imazsak TEST",
                     |        "body": "$message",
                     |        "icon": "assets/icons/icon-72x72.png",
                     |        "vibrate": [100, 50, 100],
                     |        "data": {
                     |            "dateOfArrival": ${System.currentTimeMillis()},
                     |            "primaryKey": 1
                     |        }]
                     |    }
                     |}""".stripMargin

    import cats.instances.list._
    import cats.syntax.traverse._

    EitherT
      .right(userDao.findPushSubscriptionsByUserId(userId))
      .flatMap(_.toList.traverse[Response[IO, *], Unit](sub => _sendPushNotification(sub, payload, 86400)))
      .map(_ => {})
  }

  private def _sendPushNotification(subscriptionData: UserPushSubscriptionData, payload: String, ttl: Int)(
      implicit ctx: UserLogContext
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
        logger.warn(s"PUSH FAILED! $status - $body");
      }
    }
    EitherT.right(result)
  }
}
