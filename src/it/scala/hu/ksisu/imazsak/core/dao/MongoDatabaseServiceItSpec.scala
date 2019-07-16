package hu.ksisu.imazsak.core.dao

import hu.ksisu.imazsak.AwaitUtil
import hu.ksisu.imazsak.core.dao.GroupDao.GroupListData
import hu.ksisu.imazsak.core.dao.MongoDatabaseService.MongoConfig
import hu.ksisu.imazsak.core.dao.MongoSelectors._
import hu.ksisu.imazsak.core.dao.PrayerDao.{CreatePrayerData, MinePrayerListData}
import hu.ksisu.imazsak.core.dao.UserDao.UserData
import hu.ksisu.imazsak.util.IdGeneratorCounterImpl
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}
import reactivemongo.api.{Cursor, MongoDriver}
import reactivemongo.bson.{BSON, BSONArray, BSONBoolean, BSONDocument, BSONString}
import hu.ksisu.imazsak.core.dao.BsonHelper._

import scala.concurrent.ExecutionContext.Implicits.global

class MongoDatabaseServiceItSpec extends WordSpecLike with Matchers with AwaitUtil with BeforeAndAfterEach {

  private implicit val idGenerator  = new IdGeneratorCounterImpl
  private implicit val mongoDriver  = new MongoDriver()
  private implicit val mongoConfig  = MongoConfig("mongodb://localhost/imazsak")
  private implicit val mongoService = new MongoDatabaseServiceImpl()
  private val userDao               = new UserDaoImpl()
  private val groupDao              = new GroupDaoImpl()
  private val prayerDao             = new PrayerDaoImpl()

  private val userCollection   = await(mongoService.getCollection("users"))
  private val groupCollection  = await(mongoService.getCollection("groups"))
  private val prayerCollection = await(mongoService.getCollection("prayers"))

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
    } yield ())
  }

  "mongodb instance" when {

    "CheckStatus" in {
      await(mongoService.checkStatus()) shouldEqual true
    }

    "UserDao" when {
      "#findUserData" in {
        val data = UserData("secret_id", Some("nickname"))
        await(userDao.findUserData(data.id).value) shouldEqual None
        await(userCollection.insert.one(data))
        await(userDao.findUserData(data.id).value) shouldEqual Some(data)
      }
      "#updateUserData" in {
        val userData = UserData("secret_id", Some("nickname"))
        val data     = BSON.write(userData) ++ BSONDocument("extra_data" -> BSONBoolean(true))
        await(userCollection.insert.one(data))
        await(userDao.updateUserData(userData.copy(name = Some("new_name"))))
        val result2 = await(userCollection.find(byId(userData.id), None).one[BSONDocument])
        result2 shouldBe a[Some[_]]
        result2.get.get("id") shouldEqual Some(BSONString(userData.id))
        result2.get.get("name") shouldEqual Some(BSONString("new_name"))
        result2.get.get("extra_data") shouldEqual Some(BSONBoolean(true))
      }
    }

    "GroupDao" when {
      "#findGroupsByUser" in {
        val group1 = BSONDocument(
          "id"   -> BSONString("group_1"),
          "name" -> BSONString("Group #1"),
          "members" -> BSONArray(
            BSONDocument("id" -> BSONString("user_1")),
            BSONDocument("id" -> BSONString("user_2"))
          )
        )
        val group2 = BSONDocument(
          "id"   -> BSONString("group_2"),
          "name" -> BSONString("Group #2"),
          "members" -> BSONArray(
            BSONDocument("id" -> BSONString("user_1")),
            BSONDocument("id" -> BSONString("user_3"))
          )
        )
        await(groupCollection.insert.one(group1))
        await(groupCollection.insert.one(group2))

        await(groupDao.findGroupsByUser("user_1")) shouldEqual Seq(
          GroupListData("group_1", "Group #1"),
          GroupListData("group_2", "Group #2")
        )
        await(groupDao.findGroupsByUser("user_2")) shouldEqual Seq(GroupListData("group_1", "Group #1"))
        await(groupDao.findGroupsByUser("user_3")) shouldEqual Seq(GroupListData("group_2", "Group #2"))
      }
    }

    "PrayerDao" when {
      "#createPrayer" in {
        val prayer1 = CreatePrayerData("user_1", "message1", Seq("group_1", "group_2"))
        val prayer2 = CreatePrayerData("user_1", "message2", Seq("group_2"))
        val prayer3 = CreatePrayerData("user_2", "message3", Seq("group_2", "group_3"))
        await(prayerDao.createPrayer(prayer1)) shouldEqual "1"
        await(prayerDao.createPrayer(prayer2)) shouldEqual "2"
        await(prayerDao.createPrayer(prayer3)) shouldEqual "3"

        val result = await(
          prayerCollection
            .find(BSONDocument(), None)
            .cursor[BSONDocument]()
            .collect[Seq](-1, Cursor.FailOnError[Seq[BSONDocument]]())
        )

        val resultMap = result.map(doc => doc.getId -> doc).toMap
        resultMap("1").get("userId") shouldEqual Some(BSONString("user_1"))
        resultMap("1").get("message") shouldEqual Some(BSONString("message1"))
        resultMap("1").get("groupIds") shouldEqual Some(BSONArray(BSONString("group_1"), BSONString("group_2")))
        resultMap("2").get("userId") shouldEqual Some(BSONString("user_1"))
        resultMap("2").get("message") shouldEqual Some(BSONString("message2"))
        resultMap("2").get("groupIds") shouldEqual Some(BSONArray(BSONString("group_2")))
        resultMap("3").get("userId") shouldEqual Some(BSONString("user_2"))
        resultMap("3").get("message") shouldEqual Some(BSONString("message3"))
        resultMap("3").get("groupIds") shouldEqual Some(BSONArray(BSONString("group_2"), BSONString("group_3")))
      }
      "#listMinePrayers" in {
        val prayer1 = CreatePrayerData("user_1", "message1", Seq("group_1", "group_2"))
        val prayer2 = CreatePrayerData("user_1", "message2", Seq("group_2"))
        val prayer3 = CreatePrayerData("user_2", "message3", Seq("group_2", "group_3"))
        await(prayerDao.createPrayer(prayer1)) shouldEqual "1"
        await(prayerDao.createPrayer(prayer2)) shouldEqual "2"
        await(prayerDao.createPrayer(prayer3)) shouldEqual "3"

        val result1 = await(prayerDao.findPrayerByUser("user_1"))
        result1 shouldEqual Seq(
          MinePrayerListData("1", "message1", Seq("group_1", "group_2")),
          MinePrayerListData("2", "message2", Seq("group_2"))
        )

        val result2 = await(prayerDao.findPrayerByUser("user_2"))
        result2 shouldEqual Seq(
          MinePrayerListData("3", "message3", Seq("group_2", "group_3"))
        )
      }
    }
  }
}
