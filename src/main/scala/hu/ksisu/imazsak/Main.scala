package hu.ksisu.imazsak

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import cats.effect.{ContextShift, IO}
import hu.ksisu.imazsak.util.LoggerUtil
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Main extends App {
  LoggerUtil.initBridge()

  private implicit lazy val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)

  private implicit lazy val system               = ActorSystem("imazsak-system")
  private implicit lazy val materializer         = ActorMaterializer()
  private implicit lazy val executionContext     = system.dispatcher
  private implicit lazy val cs: ContextShift[IO] = IO.contextShift(executionContext)

  private val services = new RealServices()

  private val starting = for {
    _ <- services.init()
    route = Api.createApi(services)
    server <- IO.fromFuture(IO(Http().bindAndHandle(route, "0.0.0.0", 9000)))
  } yield {
    setupShutdownHook(server)
  }

  starting.unsafeToFuture().onComplete {
    case Success(_) => logger.info("Imazsak started")
    case Failure(ex) =>
      logger.error("Imazsak starting failed", ex)
      system.terminate()
  }

  private def setupShutdownHook(server: Http.ServerBinding): Unit = {
    CoordinatedShutdown(system).addTask(CoordinatedShutdown.PhaseServiceUnbind, "http_shutdown") { () =>
      logger.info("Imazsak shutting down...")
      server.terminate(hardDeadline = 8.seconds).map(_ => Done)
    }
  }
}
