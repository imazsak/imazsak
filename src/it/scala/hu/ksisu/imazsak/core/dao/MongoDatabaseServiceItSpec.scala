package hu.ksisu.imazsak.core.dao

import hu.ksisu.imazsak.AwaitUtil
import hu.ksisu.imazsak.core.dao.BsonHelper._
import hu.ksisu.imazsak.core.dao.GroupDao.{CreateGroupData, GroupAdminListData, GroupListData, GroupMember}
import hu.ksisu.imazsak.core.dao.MongoDatabaseService.MongoConfig
import hu.ksisu.imazsak.core.dao.MongoSelectors._
import hu.ksisu.imazsak.core.dao.NotificationDao.{CreateNotificationData, NotificationListData, NotificationMeta}
import hu.ksisu.imazsak.core.dao.PrayerDao.{CreatePrayerData, GroupPrayerListData, MinePrayerListData}
import hu.ksisu.imazsak.core.dao.UserDao.{UserAdminListData, UserData}
import hu.ksisu.imazsak.util.IdGeneratorCounterImpl
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}
import reactivemongo.api.{Cursor, MongoDriver}
import reactivemongo.bson.{BSON, BSONArray, BSONBoolean, BSONDocument, BSONString}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

class MongoDatabaseServiceItSpec extends WordSpecLike with Matchers with AwaitUtil with BeforeAndAfterEach {

  private implicit val idGenerator  = new IdGeneratorCounterImpl
  private implicit val mongoDriver  = new MongoDriver()
  private implicit val mongoConfig  = MongoConfig("mongodb://localhost/imazsak")
  private implicit val mongoService = new MongoDatabaseServiceImpl()
  private val userDao               = new UserDaoImpl()
  private val groupDao              = new GroupDaoImpl()
  private val prayerDao             = new PrayerDaoImpl()
  private val notificationDao       = new NotificationDaoImpl()

  private val userCollection          = await(mongoService.getCollection("users"))
  private val groupCollection         = await(mongoService.getCollection("groups"))
  private val prayerCollection        = await(mongoService.getCollection("prayers"))
  private val notificationsCollection = await(mongoService.getCollection("notifications"))

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
      _ <- notificationsCollection.delete.one(BSONDocument())
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
      "#allUser" in {
        val user1 = UserData("secret_id1", Some("nickname1"))
        val user2 = UserData("secret_id2", None)
        val user3 = UserData("secret_id3", Some("nickname3"))
        await(userCollection.insert.many(Seq(user1, user2, user3)))
        await(userDao.allUser()) shouldEqual Seq(
          UserAdminListData("secret_id1", Some("nickname1")),
          UserAdminListData("secret_id2", None),
          UserAdminListData("secret_id3", Some("nickname3"))
        )
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
      "#findGroupsByName" in {
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

        await(groupDao.findGroupByName("Group #1").value) shouldEqual Some(GroupListData("group_1", "Group #1"))
        await(groupDao.findGroupByName("Group").value) shouldEqual None
      }
      "#allGroup" in {
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

        await(groupDao.allGroup()) shouldEqual Seq(
          GroupAdminListData("group_1", "Group #1", Seq(GroupMember("user_1"), GroupMember("user_2"))),
          GroupAdminListData("group_2", "Group #2", Seq(GroupMember("user_1"), GroupMember("user_3")))
        )
      }
      "#isMember" in {
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

        await(groupDao.isMember("group_1", "user_1")) shouldEqual true
        await(groupDao.isMember("group_1", "user_2")) shouldEqual true
        await(groupDao.isMember("group_1", "user_3")) shouldEqual false
        await(groupDao.isMember("group_2", "user_1")) shouldEqual true
        await(groupDao.isMember("group_2", "user_2")) shouldEqual false
        await(groupDao.isMember("group_2", "user_3")) shouldEqual true
        await(groupDao.isMember("group_3", "user_4")) shouldEqual false
      }
      "#createGroup" in {
        val data = CreateGroupData("Group Name", Seq(GroupMember("user 1"), GroupMember("user 2")))
        await(groupDao.createGroup(data)) shouldEqual "1"

        val result = await(
          groupCollection
            .find(BSONDocument(), None)
            .one[BSONDocument]
        )

        result shouldBe a[Some[_]]
        result.get.get("id") shouldEqual Some(BSONString("1"))
        result.get.get("name") shouldEqual Some(BSONString("Group Name"))
        result.get.get("members") shouldBe a[Some[_]]
        val members = result.get
          .get("members")
          .collect { case x: BSONArray => x.stream.toList }
          .get
          .collect { case Success(value: BSONDocument) => value.get("id") }
          .collect { case Some(x: BSONString) => x.value }
        members shouldEqual Seq("user 1", "user 2")
      }
      "#addMemberToGroup" in {
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

        await(groupDao.addMemberToGroup("group_2", GroupMember("new_user_id")))

        await(groupDao.isMember("group_1", "new_user_id")) shouldEqual false
        await(groupDao.isMember("group_2", "new_user_id")) shouldEqual true
        await(groupDao.isMember("group_2", "user_1")) shouldEqual true
        await(groupDao.isMember("group_2", "user_2")) shouldEqual false
        await(groupDao.isMember("group_2", "user_3")) shouldEqual true
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
      "#findByGroup" in {
        val prayer1 = CreatePrayerData("user_1", "message1", Seq("group_1", "group_2"))
        val prayer2 = CreatePrayerData("user_1", "message2", Seq("group_2"))
        val prayer3 = CreatePrayerData("user_2", "message3", Seq("group_2", "group_3"))
        await(prayerDao.createPrayer(prayer1)) shouldEqual "1"
        await(prayerDao.createPrayer(prayer2)) shouldEqual "2"
        await(prayerDao.createPrayer(prayer3)) shouldEqual "3"

        val result1 = await(prayerDao.findByGroup("group_1"))
        result1 shouldEqual Seq(
          GroupPrayerListData("1", "user_1", "message1")
        )

        val result2 = await(prayerDao.findByGroup("group_2"))
        result2 shouldEqual Seq(
          GroupPrayerListData("1", "user_1", "message1"),
          GroupPrayerListData("2", "user_1", "message2"),
          GroupPrayerListData("3", "user_2", "message3")
        )

        val result3 = await(prayerDao.findByGroup("group_3"))
        result3 shouldEqual Seq(
          GroupPrayerListData("3", "user_2", "message3")
        )
      }
    }

    "NotificationDao" when {
      "#createNotification and #findByUser" in {
        val data1 = CreateNotificationData("user_id1", "message1", createdAt = 1, NotificationMeta(false, None))
        val data2 = CreateNotificationData("user_id1", "message2", createdAt = 2, NotificationMeta(true, Some("type1")))
        val data3 =
          CreateNotificationData("user_id2", "message3", createdAt = 3, NotificationMeta(false, Some("type2")))
        await(notificationDao.createNotification(data1)) shouldEqual "1"
        await(notificationDao.createNotification(data2)) shouldEqual "2"
        await(notificationDao.createNotification(data3)) shouldEqual "3"
        await(notificationDao.findByUser("user_id1")) shouldEqual Seq(
          NotificationListData("1", "message1", createdAt = 1, NotificationMeta(false, None)),
          NotificationListData("2", "message2", createdAt = 2, NotificationMeta(true, Some("type1")))
        )
        await(notificationDao.findByUser("user_id2")) shouldEqual Seq(
          NotificationListData("3", "message3", createdAt = 3, NotificationMeta(false, Some("type2")))
        )
      }
      "#updateMeta" in {
        val data1 = CreateNotificationData("user_id1", "message1", createdAt = 1, NotificationMeta(false, None))
        await(notificationDao.createNotification(data1)) shouldEqual "1"
        await(notificationDao.updateMeta("1", NotificationMeta(true, Some("type999"))))
        await(notificationDao.findByUser("user_id1")) shouldEqual Seq(
          NotificationListData("1", "message1", createdAt = 1, NotificationMeta(true, Some("type999")))
        )
      }
      "#delete" in {
        val data1 = CreateNotificationData("user_id1", "message1", createdAt = 1, NotificationMeta(false, None))
        val data2 = CreateNotificationData("user_id1", "message2", createdAt = 2, NotificationMeta(true, Some("type1")))
        await(notificationDao.createNotification(data1)) shouldEqual "1"
        await(notificationDao.createNotification(data2)) shouldEqual "2"
        await(notificationDao.delete("1"))
        await(notificationDao.findByUser("user_id1")) shouldEqual Seq(
          NotificationListData("2", "message2", createdAt = 2, NotificationMeta(true, Some("type1")))
        )
      }

    }
  }
}
