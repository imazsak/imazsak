package hu.ksisu.imazsak.notification

import cats.MonadError
import cats.data.EitherT
import cats.effect.IO
import hu.ksisu.imazsak.Errors.{AppError, Response}
import hu.ksisu.imazsak.core.AmqpService
import hu.ksisu.imazsak.core.AmqpService.{AmqpQueue, AmqpQueueConfig}
import hu.ksisu.imazsak.notification.NotificationDao.{CreateNotificationData, NotificationMeta}
import hu.ksisu.imazsak.notification.NotificationService.{NotificationListResponse, PushSubscribeRequest}
import hu.ksisu.imazsak.notification.NotificationServiceImpl._
import hu.ksisu.imazsak.user.UserDao
import hu.ksisu.imazsak.user.UserDao.UserPushSubscribeData
import hu.ksisu.imazsak.util.DateTimeUtil
import hu.ksisu.imazsak.util.LoggerUtil.{LogContext, UserLogContext}
import spray.json._

class NotificationServiceImpl[F[_]: MonadError[*[_], Throwable]](
    implicit notificationDao: NotificationDao[F],
    userDao: UserDao[F],
    date: DateTimeUtil,
    amqpService: AmqpService[IO],
    configByName: String => AmqpQueueConfig
) extends NotificationService[F] {
  import cats.syntax.applicative._

  protected lazy val queue: AmqpQueue = {
    val conf = configByName("notification_service")
    amqpService.createQueue(conf)
  }

  override def init: F[Unit] = {
    queue
    ().pure[F]
  }

  def createNotification[T](notificationType: String, userId: String, message: T)(
      implicit ctx: LogContext,
      w: JsonWriter[T]
  ): Response[F, Unit] = {
    queue.send(CreateNotificationQueueMessage(notificationType, userId, message.toJson))

    val model = CreateNotificationData(
      userId,
      message.toJson.compactPrint,
      date.getCurrentTimeMillis,
      NotificationMeta(
        isRead = false,
        Option(notificationType)
      )
    )
    EitherT.right[AppError](notificationDao.createNotification(model)).map(_ => ())
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

  override def pushSubscribe(data: PushSubscribeRequest)(implicit ctx: UserLogContext): Response[F, Unit] = {
    val daoData = UserPushSubscribeData(
      data.endpoint,
      data.expirationTime,
      data.keys
    )
    EitherT.right(userDao.savePushSubscribe(ctx.userId, daoData))
  }

  override def pushUnsubscribe()(implicit ctx: UserLogContext): Response[F, Unit] = {
    EitherT.right(userDao.removePushSubscribe(ctx.userId))
  }
}

object NotificationServiceImpl {
  import spray.json.DefaultJsonProtocol._
  case class CreateNotificationQueueMessage(notificationType: String, userId: String, message: JsValue)
  implicit val createNotificationQueueMessageFormat: RootJsonFormat[CreateNotificationQueueMessage] = jsonFormat3(
    CreateNotificationQueueMessage
  )
}
