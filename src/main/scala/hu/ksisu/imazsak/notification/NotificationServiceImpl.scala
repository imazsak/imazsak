package hu.ksisu.imazsak.notification

import akka.actor.ActorSystem
import cats.MonadError
import cats.data.EitherT
import cats.effect.IO
import hu.ksisu.imazsak.Errors.{AppError, Response}
import hu.ksisu.imazsak.core.AmqpService
import hu.ksisu.imazsak.core.AmqpService.AmqpQueueConfig
import hu.ksisu.imazsak.notification.NotificationDao.{CreateNotificationData, NotificationMeta}
import hu.ksisu.imazsak.notification.NotificationService.{NotificationListResponse, PushSubscribeRequest}
import hu.ksisu.imazsak.user.UserDao
import hu.ksisu.imazsak.user.UserDao.UserPushSubscribeData
import hu.ksisu.imazsak.util.DateTimeUtil
import hu.ksisu.imazsak.util.LoggerUtil.{LogContext, UserLogContext}
import org.slf4j.LoggerFactory
import spray.json._

class NotificationServiceImpl[F[_]: MonadError[*[_], Throwable]](
    implicit notificationDao: NotificationDao[F],
    userDao: UserDao[F],
    date: DateTimeUtil,
    amqpService: AmqpService[IO],
    configByName: String => AmqpQueueConfig,
    actorSystem: ActorSystem
) extends NotificationService[F] {
  import cats.syntax.applicative._

  private implicit val logger = LoggerFactory.getLogger("NotificationService")

//  protected lazy val amqpSender: AmqpSenderWrapper = {
//    val conf = configByName("notification_service")
//    amqpService.createSenderWrapper(conf)
//  }

//  protected lazy val amqpSource = {
//    val conf = configByName("notification_service")
//    amqpService
//      .createQueueSource(conf)
//      .mapConcat { readResult =>
//        Try(readResult.bytes.utf8String.parseJson.convertTo[CreateNotificationQueueMessage])
//          .map(List(_))
//          .getOrElse(List.empty)
//      }
//      .map { msg =>
//        val model = CreateNotificationData(
//          msg.userId,
//          msg.message.compactPrint,
//          date.getCurrentTimeMillis,
//          NotificationMeta(
//            isRead = false,
//            Option(msg.notificationType)
//          )
//        )
//        logger.info(s"NOTI: $model")
//      }
//      .runWith(Sink.ignore)
//  }

  override def init: F[Unit] = {
//    amqpSender
//    amqpSource
    ().pure[F]
  }

  def createNotification[T](notificationType: String, userId: String, message: T)(
      implicit ctx: LogContext,
      w: JsonWriter[T]
  ): Response[F, Unit] = {
//    amqpSender.send(CreateNotificationQueueMessage(notificationType, userId, message.toJson))

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
