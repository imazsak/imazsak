package hu.ksisu.imazsak.core.dao

import cats.effect.IO
import hu.ksisu.imazsak.AwaitUtil
import hu.ksisu.imazsak.core.dao.MongoSelectors._
import hu.ksisu.imazsak.user.UserDao
import hu.ksisu.imazsak.user.UserDao.{UserAdminListData, UserData, UserPushSubscriptionData}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import reactivemongo.api.bson._
import reactivemongo.api.bson.collection.BSONCollection

import scala.concurrent.ExecutionContext.Implicits.global

trait UserDaoSpec {
  self: AnyWordSpecLike with Matchers with AwaitUtil =>

  protected val userDao: UserDao[IO]
  protected val userCollection: BSONCollection

  def userDaoTests(): Unit = {
    "UserDao" when {
      "#findUserData" in {
        val data = UserData("secret_id", Some("nickname"))
        userDao.findUserData(data.id).value.unsafeRunSync() shouldEqual None
        await(userCollection.insert.one(data))
        userDao.findUserData(data.id).value.unsafeRunSync() shouldEqual Some(data)
      }
      "#updateUserData" in {
        val userData = UserData("secret_id", Some("nickname"))
        val data     = BSON.writeDocument(userData).getOrElse(document()) ++ BSONDocument("extra_data" -> BSONBoolean(true))
        await(userCollection.insert.one(data))
        userDao.updateUserData(userData.copy(name = Some("new_name"))).unsafeRunSync()
        val result2 = await(userCollection.find(byId(userData.id), None).one[BSONDocument])
        result2 shouldBe a[Some[_]]
        result2.get.get("id") shouldEqual Some(BSONString(userData.id))
        result2.get.get("name") shouldEqual Some(BSONString("new_name"))
        result2.get.get("extra_data") shouldEqual Some(BSONBoolean(true))
      }
      "#allUser" in {
        val user1 = UserData("secret_id1", Some("nickname1"))
        val user2 = UserData("secret_id2", None)
        val user3 = UserData("secret_id3", Some("nickname3"))
        await(userCollection.insert.many(Seq(user1, user2, user3)))
        userDao.allUser().unsafeRunSync() shouldEqual Seq(
          UserAdminListData("secret_id1", Some("nickname1")),
          UserAdminListData("secret_id2", None),
          UserAdminListData("secret_id3", Some("nickname3"))
        )
      }
      "#findUsersByIds" in {
        val user1 = UserData("secret_id1", Some("nickname1"))
        val user2 = UserData("secret_id2", None)
        val user3 = UserData("secret_id3", Some("nickname3"))
        await(userCollection.insert.many(Seq(user1, user2, user3)))
        userDao.findUsersByIds(Seq("secret_id1", "secret_id2")).unsafeRunSync() shouldEqual Seq(
          UserAdminListData("secret_id1", Some("nickname1")),
          UserAdminListData("secret_id2", None)
        )
        userDao.findUsersByIds(Seq("secret_id3")).unsafeRunSync() shouldEqual Seq(
          UserAdminListData("secret_id3", Some("nickname3"))
        )
      }
      "#isAdmin" in {
        val userId = "secret_id"

        userDao.isAdmin(userId).unsafeRunSync() shouldBe false

        val user = UserData(userId, Some("nickname1"))
        await(userCollection.insert.one(user))
        userDao.isAdmin(userId).unsafeRunSync() shouldBe false

        await(
          userCollection.update.one(
            BSONDocument("id"   -> BSONString(userId)),
            BSONDocument("$set" -> BSONDocument("isAdmin" -> BSONBoolean(true)))
          )
        )
        userDao.isAdmin(userId).unsafeRunSync() shouldBe true
      }

      "#addPushSubscription, #findPushSubscriptionsByUserId, #removePushSubscriptionByDeviceId" in {
        val userId1 = "secret_id1"
        val user1   = UserData(userId1, Some("nickname1"))
        val userId2 = "secret_id2"
        val user2   = UserData(userId2, Some("nickname2"))
        await(userCollection.insert.one(user1))
        await(userCollection.insert.one(user2))

        val data = UserPushSubscriptionData("end+point", Some(1111L), Map("a" -> "b", "c" -> "d"))

        val deviceId1 = "devid1"
        val deviceId2 = "devid2"

        userDao.findPushSubscriptionsByUserId(userId1).unsafeRunSync() shouldEqual Seq.empty
        userDao.findPushSubscriptionsByUserId(userId2).unsafeRunSync() shouldEqual Seq.empty

        userDao.addPushSubscription(userId1, deviceId1, data).unsafeRunSync()
        userDao.addPushSubscription(userId1, deviceId2, data).unsafeRunSync()

        userDao.findPushSubscriptionsByUserId(userId1).unsafeRunSync() shouldEqual Seq(data, data)
        userDao.findPushSubscriptionsByUserId(userId2).unsafeRunSync() shouldEqual Seq.empty

        userDao.addPushSubscription(userId2, deviceId2, data).unsafeRunSync()

        userDao.findPushSubscriptionsByUserId(userId1).unsafeRunSync() shouldEqual Seq(data)
        userDao.findPushSubscriptionsByUserId(userId2).unsafeRunSync() shouldEqual Seq(data)

        userDao.removePushSubscriptionByDeviceId(deviceId1).unsafeRunSync()

        userDao.findPushSubscriptionsByUserId(userId1).unsafeRunSync() shouldEqual Seq.empty
        userDao.findPushSubscriptionsByUserId(userId2).unsafeRunSync() shouldEqual Seq(data)

        userDao.removePushSubscriptionByDeviceId(deviceId2).unsafeRunSync()
        userDao.findPushSubscriptionsByUserId(userId1).unsafeRunSync() shouldEqual Seq.empty
        userDao.findPushSubscriptionsByUserId(userId2).unsafeRunSync() shouldEqual Seq.empty
      }
    }
  }
}
