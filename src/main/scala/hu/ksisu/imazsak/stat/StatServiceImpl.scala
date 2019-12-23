package hu.ksisu.imazsak.stat

import java.text.SimpleDateFormat
import java.util.Date

import cats.Applicative
import cats.data.EitherT
import hu.ksisu.imazsak.Errors.Response
import hu.ksisu.imazsak.core.AmqpService
import hu.ksisu.imazsak.core.AmqpService.{AmqpQueueConfig, AmqpSenderWrapper}
import hu.ksisu.imazsak.prayer.PrayerService.{CreatePrayerRequest, PrayerCloseRequest}
import hu.ksisu.imazsak.stat.StatServiceImpl._
import hu.ksisu.imazsak.util.DateTimeUtil
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext
import spray.json._

class StatServiceImpl[F[_]: Applicative](
    implicit amqpService: AmqpService[F],
    configByName: String => AmqpQueueConfig,
    dateTimeUtil: DateTimeUtil
) extends StatService[F] {
  import cats.syntax.applicative._

  protected lazy val amqpSender: AmqpSenderWrapper = {
    val conf = configByName("stats_service")
    amqpService.createSenderWrapper(conf)
  }

  private val formatter = new SimpleDateFormat("yyyy-MM")

  override def init: F[Unit] = {
    amqpSender
    ().pure[F]
  }

  override def prayerCreated(data: CreatePrayerRequest)(implicit ctx: UserLogContext): Response[F, Unit] = {
    val key = "prayer-created"
    sendIncrementMessage(key, s"$key-${ctx.userId}")
    EitherT.rightT({})
  }

  override def prayerClosed(data: PrayerCloseRequest)(implicit ctx: UserLogContext): Response[F, Unit] = {
    val key = "prayer-closed"
    sendIncrementMessage(key, s"$key-${ctx.userId}")
    if (!data.message.forall(_.isEmpty)) {
      val feedbackKey = "prayer-feedback"
      sendIncrementMessage(key, s"$feedbackKey-${ctx.userId}")
    }
    EitherT.rightT({})
  }

  override def prayed(prayerId: String)(implicit ctx: UserLogContext): Response[F, Unit] = {
    val key = "prayed"
    sendIncrementMessage(key, s"$key-${ctx.userId}")
    EitherT.rightT({})
  }

  override def joinedToGroup(groupId: String)(implicit ctx: UserLogContext): Response[F, Unit] = {
    val key = "member-joined"
    sendIncrementMessage(key, s"$key-$groupId")
    EitherT.rightT({})
  }

  private def sendIncrementMessage(keys: String*): Unit = {
    keys.foreach(key => amqpSender.send(StatsIncrementMessage(key, getDateKey())))
  }

  private def getDateKey(): String = {
    val date = new Date(dateTimeUtil.getCurrentTimeMillis)
    formatter.format(date)
  }
}

object StatServiceImpl {
  import spray.json.DefaultJsonProtocol._
  case class StatsIncrementMessage(key: String, dateKey: String)
  implicit val statsIncrementMessageFormat: RootJsonFormat[StatsIncrementMessage] = jsonFormat2(StatsIncrementMessage)
}
