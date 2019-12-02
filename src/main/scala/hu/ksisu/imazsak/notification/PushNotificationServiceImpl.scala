package hu.ksisu.imazsak.notification

import java.nio.charset.StandardCharsets
import java.security.Security

import cats.data.EitherT
import cats.effect.IO
import hu.ksisu.imazsak.Errors.Response
import hu.ksisu.imazsak.notification.PushNotificationService.PushNotificationConfig
import hu.ksisu.imazsak.user.UserDao
import hu.ksisu.imazsak.user.UserDao.UserPushSubscribeData
import hu.ksisu.imazsak.util.LoggerUtil.{Logger, UserLogContext}
import nl.martijndwars.webpush._
import org.bouncycastle.jce.provider.BouncyCastleProvider

import scala.io.Source

class PushNotificationServiceImpl(implicit config: PushNotificationConfig, userDao: UserDao[IO])
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

  def sendPushNotification(userId: String, message: String)(implicit ctx: UserLogContext): Response[IO, Unit] = {
    val result = userDao
      .findPushSubscribe(userId)
      .flatMap { sub =>
        _sendPushNotification(sub, message, 86400).toOption
      }
      .getOrElse({})
    EitherT.right(result)
  }

  private def _sendPushNotification(subscriptionData: UserPushSubscribeData, payload: String, ttl: Int)(
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
      if (status != 200) {
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
