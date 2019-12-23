package hu.ksisu.imazsak.notification

import cats.MonadError
import cats.data.EitherT
import hu.ksisu.imazsak.Errors.{AppError, Response}
import hu.ksisu.imazsak.core.AmqpService
import hu.ksisu.imazsak.core.AmqpService.{AmqpQueueConfig, AmqpSenderWrapper}
import hu.ksisu.imazsak.notification.NotificationService.{NotificationInfoResponse, NotificationListResponse}
import hu.ksisu.imazsak.notification.NotificationServiceImpl.CreateNotificationQueueMessage
import hu.ksisu.imazsak.util.LoggerUtil.{LogContext, UserLogContext}
import spray.json._

class NotificationServiceImpl[F[_]: MonadError[*[_], Throwable]](
    implicit notificationDao: NotificationDao[F],
    amqpService: AmqpService[F],
    configByName: String => AmqpQueueConfig
) extends NotificationService[F] {
  import cats.syntax.applicative._

  protected lazy val amqpSender: AmqpSenderWrapper = {
    val conf = configByName("notification_service")
    amqpService.createSenderWrapper(conf)
  }

  override def init: F[Unit] = {
    amqpSender
    ().pure[F]
  }

  def createNotification[T](notificationType: String, userId: String, message: T)(
      implicit ctx: LogContext,
      w: JsonWriter[T]
  ): Response[F, Unit] = {
    EitherT.rightT(amqpSender.send(CreateNotificationQueueMessage(notificationType, userId, message.toJson)))
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
    EitherT
      .right(notificationDao.deleteByIds(ids, Some(ctx.userId)))
      .map(_ => ())
  }

  override def setUserRead(ids: Seq[String])(implicit ctx: UserLogContext): Response[F, Unit] = {
    EitherT.right(notificationDao.setRead(ids, ctx.userId))
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

    EitherT
      .right(notificationDao.countNotReadByUser(ctx.userId, Some(10)))
      .map(countToLabel)
      .map(NotificationInfoResponse)
  }
}

object NotificationServiceImpl {
  import spray.json.DefaultJsonProtocol._
  case class CreateNotificationQueueMessage(notificationType: String, userId: String, message: JsValue)
  implicit val createNotificationQueueMessageFormat: RootJsonFormat[CreateNotificationQueueMessage] = jsonFormat3(
    CreateNotificationQueueMessage
  )
}
