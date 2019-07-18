package hu.ksisu.imazsak.notification

import cats.MonadError
import cats.data.EitherT
import hu.ksisu.imazsak.Errors.Response
import hu.ksisu.imazsak.notification.NotificationDao.{CreateNotificationData, NotificationListData, NotificationMeta}
import hu.ksisu.imazsak.notification.NotificationService.CreateNotificationRequest
import hu.ksisu.imazsak.util.DateTimeUtil
import hu.ksisu.imazsak.util.LoggerUtil.{LogContext, UserLogContext}

class NotificationServiceImpl[F[_]: MonadError[?[_], Throwable]](
    implicit notificationDao: NotificationDao[F],
    date: DateTimeUtil
) extends NotificationService[F] {

  override def createNotification(data: CreateNotificationRequest)(implicit ctx: LogContext): Response[F, Unit] = {
    val model = CreateNotificationData(
      data.userId,
      data.message,
      date.getCurrentTimeMillis,
      NotificationMeta(
        isRead = false,
        data.meta.notificationType
      )
    )
    EitherT.right[Throwable](notificationDao.createNotification(model)).map(_ => ())
  }

  override def listUserNotifications()(implicit ctx: UserLogContext): Response[F, Seq[NotificationListData]] = {
    val limit = Some(10)
    EitherT.right[Throwable](notificationDao.findByUserOrderByDateDesc(ctx.userId, limit))
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
