package hu.ksisu.imazsak.core.dao

import cats.data.EitherT
import hu.ksisu.imazsak.core.Errors.WrongConfig
import hu.ksisu.imazsak.core.dao.MongoDatabaseService.MongoConfig
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}

import scala.concurrent.{ExecutionContext, Future}

class MongoDatabaseServiceImpl(implicit config: MongoConfig, ec: ExecutionContext, driver: MongoDriver)
    extends MongoDatabaseService[Future] {
  import cats.instances.future._

  private lazy val database: Future[DefaultDB] = {
    val result: EitherT[Future, Throwable, DefaultDB] = for {
      uri        <- EitherT.fromEither(MongoConnection.parseURI(config.uri).toEither)
      dbname     <- EitherT.fromOption(uri.db, WrongConfig("Database name not found!"))
      connection <- EitherT.fromEither(driver.connection(uri, None, strictUri = false).toEither)
      db         <- EitherT.right(connection.database(dbname))
    } yield db

    result.foldF(Future.failed, Future.successful)
  }

  override def init: Future[Unit] = {
    database.map(_ => ())
  }

  override def checkStatus(): Future[Boolean] = {
    database.map(_ => true).recover { case _ => false }
  }

  override def getCollection(name: String): Future[BSONCollection] = database.map(_.collection[BSONCollection](name))
}
