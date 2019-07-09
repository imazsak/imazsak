package hu.ksisu.imazsak.core.database

import cats.data.OptionT
import hu.ksisu.imazsak.AwaitUtil
import hu.ksisu.imazsak.core.dao.MongoDatabaseService.MongoConfig
import hu.ksisu.imazsak.core.dao.MongoDatabaseServiceImpl
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.{MongoConnection, MongoDriver}
import reactivemongo.bson.BSONDocument

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DatabaseServiceItSpec extends WordSpecLike with Matchers with AwaitUtil with BeforeAndAfterAll {
  import cats.instances.future._

  private implicit val mongoDriver = new MongoDriver()
  private implicit val mongoConfig = MongoConfig("mongodb://localhost/imazsak")
  private val mongoService         = new MongoDatabaseServiceImpl()
  private def getMongoCollection(name: String) =
    await({
      (for {
        uri        <- OptionT.fromOption(MongoConnection.parseURI(mongoConfig.uri).toOption)
        dbname     <- OptionT.fromOption(uri.db)
        connection <- OptionT.fromOption(mongoDriver.connection(uri, strictUri = false).toOption)
        database   <- OptionT.liftF(connection.database(dbname))
      } yield database.collection[BSONCollection](name)).getOrElseF(Future.failed(new Exception("")))
    })

  override def beforeAll(): Unit = truncateDb()

  override def afterAll(): Unit = truncateDb()

  private def truncateDb(): Unit = {
    await(for {
      _ <- getMongoCollection("users").delete().one(BSONDocument())
    } yield ())

  }

  Seq(
    "mongodb instance" -> mongoService
  ).foreach {
    case (name, service) =>
      name when {

        "CheckStatus" in {
          await(service.checkStatus()) shouldEqual true
        }
      }
  }
}
