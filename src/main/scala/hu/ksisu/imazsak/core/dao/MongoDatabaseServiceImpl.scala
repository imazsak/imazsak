package hu.ksisu.imazsak.core.dao

import cats.data.EitherT
import cats.effect.{ContextShift, IO}
import hu.ksisu.imazsak.core.Errors.WrongConfig
import hu.ksisu.imazsak.core.dao.MongoDatabaseService.MongoConfig
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.{AsyncDriver, DefaultDB, MongoConnection}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class MongoDatabaseServiceImpl(
    implicit config: MongoConfig,
    ec: ExecutionContext,
    cs: ContextShift[IO],
    driver: AsyncDriver
) extends MongoDatabaseService[IO] {
  import cats.instances.future._

  private lazy val database: Future[DefaultDB] = {
    val result: EitherT[Future, Throwable, DefaultDB] = for {
      uri        <- EitherT(MongoConnection.fromString(config.uri).map(Right(_)).recover(Left(_)))
      dbname     <- EitherT.fromOption(uri.db, WrongConfig("Database name not found!"))
      connection <- EitherT(driver.connect(uri).map(Right(_)).recover(Left(_)))
      db         <- EitherT.right(connection.database(dbname))
    } yield db

    result.foldF(Future.failed, Future.successful)
  }

  private lazy val databaseIo: IO[DefaultDB] = IO.fromFuture(IO(database))

  override def init: IO[Unit] = {
    databaseIo.map(_ => ())
  }

  override def checkStatus(): IO[Boolean] = {
    databaseIo.flatMap { db =>
      IO.async { cb =>
        db.ping().onComplete {
          case Success(r) => cb(Right(r))
          case Failure(_) => cb(Right(false))
        }
      }
    }
  }

  override def getCollection(name: String): IO[BSONCollection] = {
    databaseIo.map(_.collection[BSONCollection](name))
  }
}
