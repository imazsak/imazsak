package hu.ksisu.imazsak.notification

import cats.MonadError
import cats.data.EitherT
import hu.ksisu.imazsak.Errors.{AppError, Response}
import hu.ksisu.imazsak.core.AmqpService.{AmqpQueueConfig, AmqpSenderWrapper}
import hu.ksisu.imazsak.core.{AmqpService, CacheService}
import hu.ksisu.imazsak.notification.NotificationService._
import hu.ksisu.imazsak.notification.NotificationServiceImpl.CreateNotificationQueueMessage
import hu.ksisu.imazsak.util.LoggerUtil.{LogContext, UserLogContext}
import spray.json._
import scala.concurrent.duration._

class NotificationServiceImpl[F[_]: MonadError[*[_], Throwable]](implicit
    notificationDao: NotificationDao[F],
    cache: CacheService[F],
    amqpService: AmqpService[F],
    configByName: String => AmqpQueueConfig
) extends NotificationService[F] {
  import cats.syntax.applicative._
  import cats.syntax.functor._

  protected lazy val amqpSender: AmqpSenderWrapper = {
    val conf = configByName("notification_service")
    amqpService.createSenderWrapper(conf)
  }

  private val notificationInfoTtl = Some(2.days)

  override def init: F[Unit] = {
    amqpSender
    ().pure[F]
  }

  def createNotification[T](notificationType: String, userId: String, message: T)(implicit
      ctx: LogContext,
      w: JsonWriter[T]
  ): Response[F, Unit] = {
    for {
      _ <- EitherT.rightT(amqpSender.send(CreateNotificationQueueMessage(notificationType, userId, message.toJson)))
      _ <- EitherT.right(cache.remove(notificationInfoKey(userId)))
    } yield ()
  }

  override def listUserNotifications()(implicit ctx: UserLogContext): Response[F, Seq[NotificationListResponse]] = {
    val limit = Some(10)
    EitherT
      .right[AppError](notificationDao.findByUserOrderByDateDesc(ctx.userId, limit))
      .map(_.map { data =>
        import spray.json._
        NotificationListResponse(data.id, data.message.parseJson, data.createdAt, data.meta)
      })
  }

  override def deleteUserNotifications(ids: Seq[String])(implicit ctx: UserLogContext): Response[F, Unit] = {
    for {
      _ <- EitherT.right(notificationDao.deleteByIds(ids, Some(ctx.userId)))
      _ <- EitherT.right(cache.remove(notificationInfoKey(ctx.userId)))
    } yield ()
  }

  override def setUserRead(ids: Seq[String])(implicit ctx: UserLogContext): Response[F, Unit] = {
    for {
      _ <- EitherT.right(notificationDao.setRead(ids, ctx.userId))
      _ <- EitherT.right(cache.remove(notificationInfoKey(ctx.userId)))
    } yield ()
  }

  override def userNotificationsInfo()(implicit ctx: UserLogContext): Response[F, NotificationInfoResponse] = {
    def countToLabel(n: Long): String = {
      if (n == 0) {
        ""
      } else if (n >= 10) {
        "9+"
      } else {
        n.toString
      }
    }

    val result = cache.findOrSet(notificationInfoKey(ctx.userId), notificationInfoTtl) {
      notificationDao
        .countNotReadByUser(ctx.userId, Some(10))
        .map(countToLabel)
        .map(NotificationInfoResponse)
    }

    EitherT.right(result)
  }

  private def notificationInfoKey(userId: String) = s"notification_info_$userId"
}

object NotificationServiceImpl {
  import spray.json.DefaultJsonProtocol._
  case class CreateNotificationQueueMessage(notificationType: String, userId: String, message: JsValue)
  implicit val createNotificationQueueMessageFormat: RootJsonFormat[CreateNotificationQueueMessage] = jsonFormat3(
    CreateNotificationQueueMessage
  )

}
