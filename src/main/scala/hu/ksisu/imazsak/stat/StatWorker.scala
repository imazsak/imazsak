package hu.ksisu.imazsak.stat

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import cats.effect.IO
import hu.ksisu.imazsak.Services
import hu.ksisu.imazsak.core.AmqpService
import hu.ksisu.imazsak.core.AmqpService.AmqpQueueConfig
import hu.ksisu.imazsak.stat.StatServiceImpl._
import hu.ksisu.imazsak.util.LoggerUtil
import hu.ksisu.imazsak.util.LoggerUtil.{LogContext, Logger}
import spray.json._

import scala.concurrent.Future
import scala.util.Try

object StatWorker {
  def createWorker(services: Services[IO])(implicit as: ActorSystem): StatWorker = {
    import services._
    import services.configService._
    new StatWorker()
  }
}

class StatWorker(
    implicit statDao: StatDao[IO],
    amqpService: AmqpService[IO],
    configByName: String => AmqpQueueConfig,
    actorSystem: ActorSystem
) {

  import actorSystem.dispatcher
  private val logger                          = new Logger("StatWorker")
  private implicit val logContext: LogContext = LoggerUtil.createServiceContext("StatWorker")

  def start(): Unit = {
    logger.info("StatWorker started")
    statStream
      .map(_ => {
        logger.warn("StatWorker stopped")
      })
      .recover {
        case x => logger.error("StatWorker failed!", x)
      }
  }

  private lazy val statStream: Future[Done] = {
    val conf = configByName("stat_service")
    amqpService
      .createQueueSource(conf)
      .mapConcat { readResult =>
        Try(readResult.bytes.utf8String.parseJson.convertTo[StatsIncrementMessage])
          .map(List(_))
          .getOrElse(List.empty)
      }
      .mapAsync(2) { msg => // TODO group by msg key
        statDao.incrementStat(msg.key, msg.dateKey, counter = 1L).unsafeToFuture()
      }
      .runWith(Sink.ignore)
  }
}
