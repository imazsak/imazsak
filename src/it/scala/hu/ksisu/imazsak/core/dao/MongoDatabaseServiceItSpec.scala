package hu.ksisu.imazsak.core.dao

import cats.effect.IO
import hu.ksisu.imazsak.AwaitUtil
import hu.ksisu.imazsak.core.config.ServerConfigImpl
import hu.ksisu.imazsak.core.dao.MongoDatabaseService.MongoConfig
import hu.ksisu.imazsak.feedback.FeedbackDaoImpl
import hu.ksisu.imazsak.group.GroupDaoImpl
import hu.ksisu.imazsak.notification.NotificationDaoImpl
import hu.ksisu.imazsak.prayer.PrayerDaoImpl
import hu.ksisu.imazsak.stat.StatDaoImpl
import hu.ksisu.imazsak.token.TokenDaoImpl
import hu.ksisu.imazsak.user.UserDaoImpl
import hu.ksisu.imazsak.util.IdGeneratorCounterImpl
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import reactivemongo.api.AsyncDriver
import reactivemongo.api.bson._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

class MongoDatabaseServiceItSpec
    extends AnyWordSpecLike
    with Matchers
    with AwaitUtil
    with BeforeAndAfterEach
    with UserDaoSpec
    with GroupDaoSpec
    with PrayerDaoSpec
    with NotificationDaoSpec
    with FeedbackDaoSpec
    with TokenDaoSpec
    with StatDaoSpec {
  import cats.instances.try_._
  private val conf = new ServerConfigImpl[Try]

  private implicit val idGenerator  = new IdGeneratorCounterImpl
  private implicit val mongoDriver  = new AsyncDriver()
  private implicit val mongoConfig  = MongoConfig(conf.getMongoConfig.uri)
  private implicit val contextShift = IO.contextShift(implicitly[ExecutionContext])
  private implicit val mongoService = new MongoDatabaseServiceImpl()
  protected val userDao             = new UserDaoImpl()
  protected val groupDao            = new GroupDaoImpl()
  protected val prayerDao           = new PrayerDaoImpl()
  protected val notificationDao     = new NotificationDaoImpl()
  protected val feedbackDao         = new FeedbackDaoImpl()
  protected val tokenDao            = new TokenDaoImpl()
  protected val statDao             = new StatDaoImpl()

  protected val userCollection         = mongoService.getCollection("users").unsafeRunSync()
  protected val groupCollection        = mongoService.getCollection("groups").unsafeRunSync()
  protected val prayerCollection       = mongoService.getCollection("prayers").unsafeRunSync()
  protected val notificationCollection = mongoService.getCollection("notifications").unsafeRunSync()
  protected val feedbackCollection     = mongoService.getCollection("feedback").unsafeRunSync()
  protected val tokenCollection        = mongoService.getCollection("tokens").unsafeRunSync()
  protected val statCollection         = mongoService.getCollection("stats").unsafeRunSync()

  override def beforeEach(): Unit = {
    idGenerator.reset()
    truncateDb()
  }

  override def afterEach(): Unit = truncateDb()

  private def truncateDb(): Unit = {
    await(for {
      _ <- userCollection.delete.one(BSONDocument())
      _ <- groupCollection.delete.one(BSONDocument())
      _ <- prayerCollection.delete.one(BSONDocument())
      _ <- notificationCollection.delete.one(BSONDocument())
      _ <- feedbackCollection.delete.one(BSONDocument())
      _ <- tokenCollection.delete.one(BSONDocument())
      _ <- statCollection.delete.one(BSONDocument())
    } yield ())
  }

  "mongodb instance" when {

    "CheckStatus" in {
      mongoService.checkStatus().unsafeRunSync() shouldEqual true
    }
    userDaoTests()
    groupDaoTests()
    prayerDaoTests()
    notificationDaoTests()
    feedbackDaoTests()
    tokenDaoTests()
    statDaoTests()
  }
}
