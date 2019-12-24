package hu.ksisu.imazsak.notification

import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import cats.effect.IO
import hu.ksisu.imazsak.Services
import hu.ksisu.imazsak.core.AmqpService
import hu.ksisu.imazsak.core.AmqpService.AmqpQueueConfig
import hu.ksisu.imazsak.core.healthcheck.HealthCheckService
import hu.ksisu.imazsak.notification.NotificationDao.{CreateNotificationData, NotificationMeta}
import hu.ksisu.imazsak.notification.NotificationServiceImpl.CreateNotificationQueueMessage
import hu.ksisu.imazsak.util.LoggerUtil.{LogContext, Logger}
import hu.ksisu.imazsak.util.{DateTimeUtil, LoggerUtil}
import spray.json._

import scala.util.Try

object NotificationWorker {
  def createWorker(services: Services[IO])(implicit as: ActorSystem): NotificationWorker = {
    import services._
    import services.configService._
    new NotificationWorker()
  }
}

class NotificationWorker(
    implicit notificationDao: NotificationDao[IO],
    pushNotificationService: PushNotificationService[IO],
    amqpService: AmqpService[IO],
    configByName: String => AmqpQueueConfig,
    actorSystem: ActorSystem,
    date: DateTimeUtil,
    healthCheckService: HealthCheckService[IO]
) {
  import actorSystem.dispatcher
  private val logger                          = new Logger("NotificationWorker")
  private implicit val logContext: LogContext = LoggerUtil.createServiceContext("NotificationWorker")

  private val status = new AtomicBoolean(false)

  def start(): Unit = {
    healthCheckService.addModule("notification_worker", () => IO(status.get()))
    status.set(true)
    logger.info("NotificationWorker started")
    notificationStream
      .map(_ => {
        logger.warn("NotificationWorker stopped")
        status.set(false)
      })
      .recover {
        case x =>
          logger.error("NotificationWorker failed!", x)
          status.set(false)
      }
  }

  private lazy val notificationStream = {
    val conf = configByName("notification_service")
    amqpService
      .createQueueSource(conf)
      .mapConcat { readResult =>
        Try(readResult.bytes.utf8String.parseJson.convertTo[CreateNotificationQueueMessage])
          .map(List(_))
          .getOrElse(List.empty)
      }
      .mapAsync(10) { msg =>
        val model = CreateNotificationData(
          msg.userId,
          msg.message.compactPrint,
          date.getCurrentTimeMillis,
          NotificationMeta(
            isRead = false,
            Option(msg.notificationType)
          )
        )
        processNotification(model).unsafeToFuture().recover {
          case x => logger.warn("Notification process failed", x)
        }
      }
      .runWith(Sink.ignore)
  }

  private def processNotification(model: CreateNotificationData): IO[Unit] = {
    for {
      notiId <- notificationDao.createNotification(model)
      _      <- pushNotificationService.sendNotification(notiId, model).value
    } yield ()
  }

}
