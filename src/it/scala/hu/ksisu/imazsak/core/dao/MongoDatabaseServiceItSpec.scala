package hu.ksisu.imazsak.core.dao

import hu.ksisu.imazsak.AwaitUtil
import hu.ksisu.imazsak.core.dao.GroupDao.GroupListData
import hu.ksisu.imazsak.core.dao.MongoDatabaseService.MongoConfig
import hu.ksisu.imazsak.core.dao.MongoSelectors._
import hu.ksisu.imazsak.core.dao.UserDao.UserData
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}
import reactivemongo.api.MongoDriver
import reactivemongo.bson.{BSON, BSONArray, BSONBoolean, BSONDocument, BSONString}

import scala.concurrent.ExecutionContext.Implicits.global

class MongoDatabaseServiceItSpec extends WordSpecLike with Matchers with AwaitUtil with BeforeAndAfterEach {

  private implicit val mongoDriver  = new MongoDriver()
  private implicit val mongoConfig  = MongoConfig("mongodb://localhost/imazsak")
  private implicit val mongoService = new MongoDatabaseServiceImpl()
  private val userDao               = new UserDaoImpl()
  private val groupDao              = new GroupDaoImpl()

  private val userCollection  = await(mongoService.getCollection("users"))
  private val groupCollection = await(mongoService.getCollection("groups"))

  override def beforeEach(): Unit = truncateDb()

  override def afterEach(): Unit = truncateDb()

  private def truncateDb(): Unit = {
    await(for {
      _ <- userCollection.delete.one(BSONDocument())
      _ <- groupCollection.delete.one(BSONDocument())
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
  }
}
