package hu.ksisu.imazsak.core.dao

import cats.effect.IO
import hu.ksisu.imazsak.AwaitUtil
import hu.ksisu.imazsak.core.dao.BsonHelper._
import hu.ksisu.imazsak.prayer.PrayerDao
import hu.ksisu.imazsak.prayer.PrayerDao.{
  CreatePrayerData,
  GroupPrayerListData,
  MyPrayerListData,
  PrayerDetailsData,
  PrayerUpdateData,
  PrayerWithPrayUserData
}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import reactivemongo.api.Cursor
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{BSONArray, BSONDocument, BSONInteger, BSONString, _}

import scala.concurrent.ExecutionContext

trait PrayerDaoSpec {
  self: AnyWordSpecLike with Matchers with AwaitUtil =>

  protected val prayerDao: PrayerDao[IO]
  protected val prayerCollection: BSONCollection

  def prayerDaoTests()(implicit ec: ExecutionContext): Unit = {
    "PrayerDao" when {
      "#createPrayer" in {
        val prayer1 = CreatePrayerData("user_1", "message1", Seq("group_1", "group_2"))
        val prayer2 = CreatePrayerData("user_1", "message2", Seq("group_2"))
        val prayer3 = CreatePrayerData("user_2", "message3", Seq("group_2", "group_3"))
        prayerDao.createPrayer(prayer1).unsafeRunSync() shouldEqual "1"
        prayerDao.createPrayer(prayer2).unsafeRunSync() shouldEqual "2"
        prayerDao.createPrayer(prayer3).unsafeRunSync() shouldEqual "3"

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
      "#findPrayerByUser" in {
        val prayer1 = CreatePrayerData("user_1", "message1", Seq("group_1", "group_2"))
        val prayer2 = CreatePrayerData("user_1", "message2", Seq("group_2"))
        val prayer3 = CreatePrayerData("user_2", "message3", Seq("group_2", "group_3"))
        prayerDao.createPrayer(prayer1).unsafeRunSync() shouldEqual "1"
        prayerDao.createPrayer(prayer2).unsafeRunSync() shouldEqual "2"
        prayerDao.createPrayer(prayer3).unsafeRunSync() shouldEqual "3"
        prayerDao.incrementPrayCount("userid", "2").unsafeRunSync()
        prayerDao.incrementPrayCount("userid", "2").unsafeRunSync()

        val result1 = prayerDao.findPrayerByUser("user_1").unsafeRunSync()
        result1.map(_.copy(createdAt = 0)) shouldEqual Seq(
          MyPrayerListData("1", "message1", Seq("group_1", "group_2"), 0, 0),
          MyPrayerListData("2", "message2", Seq("group_2"), 2, 0)
        )
        val currentTime = System.currentTimeMillis()
        val d           = 1000L
        val startTime   = currentTime - d
        val endTime     = currentTime + d
        result1.map(_.createdAt).forall(time => startTime <= time && time <= endTime) shouldBe true

        val result2 = prayerDao.findPrayerByUser("user_2").unsafeRunSync()
        result2.map(_.copy(createdAt = 0)) shouldEqual Seq(
          MyPrayerListData("3", "message3", Seq("group_2", "group_3"), 0, 0)
        )
        result2.map(_.createdAt).forall(time => startTime <= time && time <= endTime) shouldBe true
      }
      "#findByGroup" in {
        val prayer1 = CreatePrayerData("user_1", "message1", Seq("group_1", "group_2"))
        val prayer2 = CreatePrayerData("user_1", "message2", Seq("group_2"))
        val prayer3 = CreatePrayerData("user_2", "message3", Seq("group_2", "group_3"))
        prayerDao.createPrayer(prayer1).unsafeRunSync() shouldEqual "1"
        prayerDao.createPrayer(prayer2).unsafeRunSync() shouldEqual "2"
        prayerDao.createPrayer(prayer3).unsafeRunSync() shouldEqual "3"

        val result1 = prayerDao.findByGroup("group_1").unsafeRunSync()
        result1 shouldEqual Seq(
          GroupPrayerListData("1", "user_1", "message1")
        )

        val result2 = prayerDao.findByGroup("group_2").unsafeRunSync()
        result2 shouldEqual Seq(
          GroupPrayerListData("1", "user_1", "message1"),
          GroupPrayerListData("2", "user_1", "message2"),
          GroupPrayerListData("3", "user_2", "message3")
        )

        val result3 = prayerDao.findByGroup("group_3").unsafeRunSync()
        result3 shouldEqual Seq(
          GroupPrayerListData("3", "user_2", "message3")
        )
      }
      "#incrementPrayCount" in {
        val prayUser1 = "PU1"
        val prayUser2 = "PU2"
        val prayer1   = CreatePrayerData("user_1", "message1", Seq("group_1", "group_2"))
        val prayer2   = CreatePrayerData("user_1", "message2", Seq("group_2"))
        prayerDao.createPrayer(prayer1).unsafeRunSync() shouldEqual "1"
        prayerDao.createPrayer(prayer2).unsafeRunSync() shouldEqual "2"
        prayerDao.incrementPrayCount(prayUser1, "2").unsafeRunSync()
        prayerDao.incrementPrayCount(prayUser1, "2").unsafeRunSync()
        prayerDao.incrementPrayCount(prayUser2, "2").unsafeRunSync()
        val result = await(
          prayerCollection
            .find(BSONDocument(), None)
            .cursor[BSONDocument]()
            .collect[Seq](-1, Cursor.FailOnError[Seq[BSONDocument]]())
        )

        val resultMap = result.map(doc => doc.getId -> doc).toMap
        resultMap("1").get("prayCount") shouldEqual None
        resultMap("1").get("prayUsers") shouldEqual None
        resultMap("2").get("prayCount") shouldEqual Some(BSONInteger(3))
        resultMap("2").get("prayUsers") shouldEqual Some(BSONArray(BSONString(prayUser1), BSONString(prayUser2)))
      }
      "#findByGroupIds" in {
        val prayer1 = CreatePrayerData("user_1", "message1", Seq("group_1", "group_2"))
        val prayer2 = CreatePrayerData("user_1", "message2", Seq("group_2"))
        val prayer3 = CreatePrayerData("user_2", "message3", Seq("group_2", "group_3"))
        prayerDao.createPrayer(prayer1).unsafeRunSync() shouldEqual "1"
        prayerDao.createPrayer(prayer2).unsafeRunSync() shouldEqual "2"
        prayerDao.createPrayer(prayer3).unsafeRunSync() shouldEqual "3"
        prayerDao.incrementPrayCount("userid", "2").unsafeRunSync()
        prayerDao.incrementPrayCount("userid", "2").unsafeRunSync()
        prayerDao.incrementPrayCount("userid", "3").unsafeRunSync()
        prayerDao.findNextsByGroups(Seq("group_1"), "").unsafeRunSync().map(_.id) shouldEqual Seq("1")
        prayerDao.findNextsByGroups(Seq("group_2"), "").unsafeRunSync().map(_.id) shouldEqual Seq("1", "3", "2")
        prayerDao.findNextsByGroups(Seq("group_1", "group_3"), "").unsafeRunSync().map(_.id) shouldEqual Seq("1", "3")
        prayerDao.findNextsByGroups(Seq("group_2"), "", Some(1)).unsafeRunSync().map(_.id) shouldEqual Seq("1")
        prayerDao.findNextsByGroups(Seq("group_2"), "", Some(2)).unsafeRunSync().map(_.id) shouldEqual Seq("1", "3")
        // exclude user
        prayerDao.findNextsByGroups(Seq("group_2"), "user_1").unsafeRunSync().map(_.id) shouldEqual Seq("3")
        prayerDao.findNextsByGroups(Seq("group_2"), "user_2").unsafeRunSync().map(_.id) shouldEqual Seq("1", "2")

        // filter out non requested groups
        prayerDao.findNextsByGroups(Seq("group_2", "group_3"), "").unsafeRunSync().map(_.groupIds) shouldEqual Seq(
          Seq("group_2"),
          Seq("group_2", "group_3"),
          Seq("group_2")
        )

      }
      "#findById" in {
        val prayer1 = CreatePrayerData("user_1", "message1", Seq("group_1", "group_2"))
        val prayer2 = CreatePrayerData("user_1", "message2", Seq("group_2"))
        prayerDao.createPrayer(prayer1).unsafeRunSync() shouldEqual "1"
        prayerDao.createPrayer(prayer2).unsafeRunSync() shouldEqual "2"

        val result1 = prayerDao.findById("1").value.unsafeRunSync()
        result1 shouldEqual Some(
          GroupPrayerListData("1", "user_1", "message1")
        )
        val result2 = prayerDao.findById("2").value.unsafeRunSync()
        result2 shouldEqual Some(
          GroupPrayerListData("2", "user_1", "message2")
        )
        val result3 = prayerDao.findById("3").value.unsafeRunSync()
        result3 shouldEqual None
      }
      "#findWithPrayUserListById" in {
        val prayer1 = CreatePrayerData("user_1", "message1", Seq("group_1", "group_2"))
        val prayer2 = CreatePrayerData("user_1", "message2", Seq("group_2"))
        prayerDao.createPrayer(prayer1).unsafeRunSync() shouldEqual "1"
        prayerDao.createPrayer(prayer2).unsafeRunSync() shouldEqual "2"
        prayerDao.incrementPrayCount("p_user_9", "2").unsafeRunSync()
        prayerDao.incrementPrayCount("p_user_8", "2").unsafeRunSync()
        prayerDao.incrementPrayCount("p_user_8", "2").unsafeRunSync()

        val result1 = prayerDao.findWithPrayUserListById("1").value.unsafeRunSync()
        result1 shouldEqual Some(
          PrayerWithPrayUserData("user_1", "message1", Seq(), Seq("group_1", "group_2"))
        )
        val result2 = prayerDao.findWithPrayUserListById("2").value.unsafeRunSync()
        result2 shouldEqual Some(
          PrayerWithPrayUserData("user_1", "message2", Seq("p_user_9", "p_user_8"), Seq("group_2"))
        )
        val result3 = prayerDao.findWithPrayUserListById("3").value.unsafeRunSync()
        result3 shouldEqual None
      }
      "#delete" in {
        val prayer1 = CreatePrayerData("user_1", "message1", Seq("group_1", "group_2"))
        prayerDao.createPrayer(prayer1).unsafeRunSync() shouldEqual "1"
        prayerDao.delete("1").unsafeRunSync()
        val result = prayerDao.findById("1").value.unsafeRunSync()
        result shouldEqual None
      }
      "#addUpdate and #findByIdWithUpdates" in {
        val groups  = Seq("group_1", "group_2")
        val prayer1 = CreatePrayerData("user_1", "message1", groups)
        prayerDao.createPrayer(prayer1).unsafeRunSync() shouldEqual "1"

        val result1 = prayerDao.findByIdWithUpdates("1").value.unsafeRunSync()
        result1.map(_.copy(createdAt = 0)) shouldEqual Some(
          PrayerDetailsData("1", "user_1", groups, "message1", 0, Seq())
        )

        val update1 = PrayerUpdateData("message2", 2)
        prayerDao.addUpdate("1", update1).unsafeRunSync()

        val result2 = prayerDao.findByIdWithUpdates("1").value.unsafeRunSync()
        result2.map(_.copy(createdAt = 0)) shouldEqual Some(
          PrayerDetailsData("1", "user_1", groups, "message1", 0, Seq(update1))
        )

        val update2 = PrayerUpdateData("message3", 3)
        prayerDao.addUpdate("1", update2).unsafeRunSync()

        val result3 = prayerDao.findByIdWithUpdates("1").value.unsafeRunSync()
        result3.map(_.copy(createdAt = 0)) shouldEqual Some(
          PrayerDetailsData("1", "user_1", groups, "message1", 0, Seq(update1, update2))
        )
      }
    }
  }
}
