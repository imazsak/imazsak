package hu.ksisu.imazsak.notification

import cats.MonadError
import cats.data.EitherT
import hu.ksisu.imazsak.Errors.{AppError, Response}
import hu.ksisu.imazsak.notification.NotificationDao.{CreateNotificationData, NotificationMeta}
import hu.ksisu.imazsak.notification.NotificationService.NotificationListResponse
import hu.ksisu.imazsak.util.DateTimeUtil
import hu.ksisu.imazsak.util.LoggerUtil.{LogContext, UserLogContext}
import spray.json.JsonWriter

class NotificationServiceImpl[F[_]: MonadError[?[_], Throwable]](
    implicit notificationDao: NotificationDao[F],
    date: DateTimeUtil
) extends NotificationService[F] {

  def createNotification[T](notificationType: String, userId: String, message: T)(
      implicit ctx: LogContext,
      w: JsonWriter[T]
  ): Response[F, Unit] = {
    import spray.json._
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

}
