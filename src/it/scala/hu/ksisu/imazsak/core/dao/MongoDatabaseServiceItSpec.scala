package hu.ksisu.imazsak.core.dao

import cats.effect.IO
import hu.ksisu.imazsak.AwaitUtil
import hu.ksisu.imazsak.core.config.ServerConfigImpl
import hu.ksisu.imazsak.core.dao.BsonHelper._
import hu.ksisu.imazsak.core.dao.MongoDatabaseService.MongoConfig
import hu.ksisu.imazsak.core.dao.MongoSelectors._
import hu.ksisu.imazsak.feedback.FeedbackDao.CreateFeedbackData
import hu.ksisu.imazsak.feedback.FeedbackDaoImpl
import hu.ksisu.imazsak.group.GroupDao.{CreateGroupData, GroupAdminListData, GroupListData, GroupMember}
import hu.ksisu.imazsak.group.GroupDaoImpl
import hu.ksisu.imazsak.notification.NotificationDao.{CreateNotificationData, NotificationListData, NotificationMeta}
import hu.ksisu.imazsak.notification.NotificationDaoImpl
import hu.ksisu.imazsak.prayer.PrayerDao.{CreatePrayerData, GroupPrayerListData, MyPrayerListData}
import hu.ksisu.imazsak.prayer.PrayerDaoImpl
import hu.ksisu.imazsak.user.UserDao.{UserAdminListData, UserData}
import hu.ksisu.imazsak.user.UserDaoImpl
import hu.ksisu.imazsak.util.IdGeneratorCounterImpl
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpecLike}
import reactivemongo.api.{Cursor, MongoDriver}
import reactivemongo.bson.{BSON, BSONArray, BSONBoolean, BSONDocument, BSONLong, BSONString}

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Try}

class MongoDatabaseServiceItSpec extends WordSpecLike with Matchers with AwaitUtil with BeforeAndAfterEach {
  import cats.instances.try_._
  private val conf = new ServerConfigImpl[Try]

  private implicit val idGenerator  = new IdGeneratorCounterImpl
  private implicit val mongoDriver  = new MongoDriver()
  private implicit val mongoConfig  = MongoConfig(conf.getMongoConfig.uri)
  private implicit val contextShift = IO.contextShift(implicitly[ExecutionContext])
  private implicit val mongoService = new MongoDatabaseServiceImpl()
  private val userDao               = new UserDaoImpl()
  private val groupDao              = new GroupDaoImpl()
  private val prayerDao             = new PrayerDaoImpl()
  private val notificationDao       = new NotificationDaoImpl()
  private val feedbackDao           = new FeedbackDaoImpl()

  private val userCollection          = mongoService.getCollection("users").unsafeRunSync()
  private val groupCollection         = mongoService.getCollection("groups").unsafeRunSync()
  private val prayerCollection        = mongoService.getCollection("prayers").unsafeRunSync()
  private val notificationsCollection = mongoService.getCollection("notifications").unsafeRunSync()
  private val feedbackCollection      = mongoService.getCollection("feedback").unsafeRunSync()

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
      _ <- feedbackCollection.delete.one(BSONDocument())
    } yield ())
  }

  "mongodb instance" when {

    "CheckStatus" in {
      mongoService.checkStatus().unsafeRunSync() shouldEqual true
    }

    "UserDao" when {
      "#findUserData" in {
        val data = UserData("secret_id", Some("nickname"))
        userDao.findUserData(data.id).value.unsafeRunSync() shouldEqual None
        await(userCollection.insert.one(data))
        userDao.findUserData(data.id).value.unsafeRunSync() shouldEqual Some(data)
      }
      "#updateUserData" in {
        val userData = UserData("secret_id", Some("nickname"))
        val data     = BSON.write(userData) ++ BSONDocument("extra_data" -> BSONBoolean(true))
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

        groupDao.findGroupsByUser("user_1").unsafeRunSync() shouldEqual Seq(
          GroupListData("group_1", "Group #1"),
          GroupListData("group_2", "Group #2")
        )
        groupDao.findGroupsByUser("user_2").unsafeRunSync() shouldEqual Seq(GroupListData("group_1", "Group #1"))
        groupDao.findGroupsByUser("user_3").unsafeRunSync() shouldEqual Seq(GroupListData("group_2", "Group #2"))
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

        groupDao.findGroupByName("Group #1").value.unsafeRunSync() shouldEqual Some(
          GroupListData("group_1", "Group #1")
        )
        groupDao.findGroupByName("Group").value.unsafeRunSync() shouldEqual None
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

        groupDao.allGroup().unsafeRunSync() shouldEqual Seq(
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

        groupDao.isMember("group_1", "user_1").unsafeRunSync() shouldEqual true
        groupDao.isMember("group_1", "user_2").unsafeRunSync() shouldEqual true
        groupDao.isMember("group_1", "user_3").unsafeRunSync() shouldEqual false
        groupDao.isMember("group_2", "user_1").unsafeRunSync() shouldEqual true
        groupDao.isMember("group_2", "user_2").unsafeRunSync() shouldEqual false
        groupDao.isMember("group_2", "user_3").unsafeRunSync() shouldEqual true
        groupDao.isMember("group_3", "user_4").unsafeRunSync() shouldEqual false
      }
      "#createGroup" in {
        val data = CreateGroupData("Group Name", Seq(GroupMember("user 1"), GroupMember("user 2")))
        groupDao.createGroup(data).unsafeRunSync() shouldEqual "1"

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

        groupDao.addMemberToGroup("group_2", GroupMember("new_user_id")).unsafeRunSync()

        groupDao.isMember("group_1", "new_user_id").unsafeRunSync() shouldEqual false
        groupDao.isMember("group_2", "new_user_id").unsafeRunSync() shouldEqual true
        groupDao.isMember("group_2", "user_1").unsafeRunSync() shouldEqual true
        groupDao.isMember("group_2", "user_2").unsafeRunSync() shouldEqual false
        groupDao.isMember("group_2", "user_3").unsafeRunSync() shouldEqual true
      }
    }

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
      "#listMinePrayers" in {
        val prayer1 = CreatePrayerData("user_1", "message1", Seq("group_1", "group_2"))
        val prayer2 = CreatePrayerData("user_1", "message2", Seq("group_2"))
        val prayer3 = CreatePrayerData("user_2", "message3", Seq("group_2", "group_3"))
        prayerDao.createPrayer(prayer1).unsafeRunSync() shouldEqual "1"
        prayerDao.createPrayer(prayer2).unsafeRunSync() shouldEqual "2"
        prayerDao.createPrayer(prayer3).unsafeRunSync() shouldEqual "3"

        val result1 = prayerDao.findPrayerByUser("user_1").unsafeRunSync()
        result1 shouldEqual Seq(
          MyPrayerListData("1", "message1", Seq("group_1", "group_2")),
          MyPrayerListData("2", "message2", Seq("group_2"))
        )

        val result2 = prayerDao.findPrayerByUser("user_2").unsafeRunSync()
        result2 shouldEqual Seq(
          MyPrayerListData("3", "message3", Seq("group_2", "group_3"))
        )
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
    }

    "NotificationDao" when {
      "#createNotification and #findByUser(OrderByDateDesc)" in {
        val data1 = CreateNotificationData("user_id1", "message1", createdAt = 1, NotificationMeta(false, None))
        val data2 = CreateNotificationData("user_id1", "message2", createdAt = 2, NotificationMeta(true, Some("type1")))
        val data3 =
          CreateNotificationData("user_id2", "message3", createdAt = 3, NotificationMeta(false, Some("type2")))
        notificationDao.createNotification(data1).unsafeRunSync() shouldEqual "1"
        notificationDao.createNotification(data2).unsafeRunSync() shouldEqual "2"
        notificationDao.createNotification(data3).unsafeRunSync() shouldEqual "3"
        notificationDao.findByUser("user_id1").unsafeRunSync() shouldEqual Seq(
          NotificationListData("1", "message1", createdAt = 1, NotificationMeta(false, None)),
          NotificationListData("2", "message2", createdAt = 2, NotificationMeta(true, Some("type1")))
        )
        notificationDao.findByUserOrderByDateDesc("user_id1").unsafeRunSync() shouldEqual Seq(
          NotificationListData("2", "message2", createdAt = 2, NotificationMeta(true, Some("type1"))),
          NotificationListData("1", "message1", createdAt = 1, NotificationMeta(false, None))
        )
        notificationDao.findByUserOrderByDateDesc("user_id1", limit = Some(1)).unsafeRunSync() shouldEqual Seq(
          NotificationListData("2", "message2", createdAt = 2, NotificationMeta(true, Some("type1")))
        )
        notificationDao.findByUser("user_id2").unsafeRunSync() shouldEqual Seq(
          NotificationListData("3", "message3", createdAt = 3, NotificationMeta(false, Some("type2")))
        )
      }
      "#updateMeta" in {
        val data1 = CreateNotificationData("user_id1", "message1", createdAt = 1, NotificationMeta(false, None))
        notificationDao.createNotification(data1).unsafeRunSync() shouldEqual "1"
        notificationDao.updateMeta("1", NotificationMeta(true, Some("type999"))).unsafeRunSync()
        notificationDao.findByUser("user_id1").unsafeRunSync() shouldEqual Seq(
          NotificationListData("1", "message1", createdAt = 1, NotificationMeta(true, Some("type999")))
        )
      }
      "#deleteById - without user" in {
        val data1 = CreateNotificationData("user_id1", "message1", createdAt = 1, NotificationMeta(false, None))
        val data2 = CreateNotificationData("user_id1", "message2", createdAt = 2, NotificationMeta(true, Some("type1")))
        val data3 = CreateNotificationData("user_id1", "message3", createdAt = 3, NotificationMeta(true, Some("type2")))
        notificationDao.createNotification(data1).unsafeRunSync() shouldEqual "1"
        notificationDao.createNotification(data2).unsafeRunSync() shouldEqual "2"
        notificationDao.createNotification(data3).unsafeRunSync() shouldEqual "3"
        notificationDao.deleteByIds(Seq("1", "3", "999")).unsafeRunSync() shouldEqual 2
        notificationDao.deleteByIds(Seq("1", "3", "999")).unsafeRunSync() shouldEqual 0
        notificationDao.findByUser("user_id1").unsafeRunSync() shouldEqual Seq(
          NotificationListData("2", "message2", createdAt = 2, NotificationMeta(true, Some("type1")))
        )
      }
      "#deleteById - with user" in {
        val data1 = CreateNotificationData("user_id1", "message1", createdAt = 1, NotificationMeta(false, None))
        val data2 = CreateNotificationData("user_id2", "message2", createdAt = 2, NotificationMeta(true, Some("type1")))
        val data3 = CreateNotificationData("user_id2", "message3", createdAt = 3, NotificationMeta(true, Some("type2")))
        notificationDao.createNotification(data1).unsafeRunSync() shouldEqual "1"
        notificationDao.createNotification(data2).unsafeRunSync() shouldEqual "2"
        notificationDao.createNotification(data3).unsafeRunSync() shouldEqual "3"
        notificationDao.deleteByIds(Seq("1", "2", "3"), Some("user_id1")).unsafeRunSync() shouldEqual 1
        notificationDao.findByUser("user_id1").unsafeRunSync() shouldEqual Seq()
        notificationDao.findByUser("user_id2").unsafeRunSync() shouldEqual Seq(
          NotificationListData("2", "message2", createdAt = 2, NotificationMeta(true, Some("type1"))),
          NotificationListData("3", "message3", createdAt = 3, NotificationMeta(true, Some("type2")))
        )
      }
      "#setRead" in {
        val data1 = CreateNotificationData("user_id1", "message1", createdAt = 1, NotificationMeta(false, None))
        val data2 =
          CreateNotificationData("user_id2", "message2", createdAt = 2, NotificationMeta(false, Some("type1")))
        val data3 =
          CreateNotificationData("user_id1", "message3", createdAt = 3, NotificationMeta(false, Some("type2")))
        val data4 =
          CreateNotificationData("user_id1", "message4", createdAt = 4, NotificationMeta(false, Some("type3")))
        notificationDao.createNotification(data1).unsafeRunSync() shouldEqual "1"
        notificationDao.createNotification(data2).unsafeRunSync() shouldEqual "2"
        notificationDao.createNotification(data3).unsafeRunSync() shouldEqual "3"
        notificationDao.createNotification(data4).unsafeRunSync() shouldEqual "4"
        notificationDao.setRead(Seq("1", "3"), "user_id1").unsafeRunSync()
        notificationDao.findByUser("user_id1").unsafeRunSync() shouldEqual Seq(
          NotificationListData("1", "message1", createdAt = 1, NotificationMeta(true, None)),
          NotificationListData("3", "message3", createdAt = 3, NotificationMeta(true, Some("type2"))),
          NotificationListData("4", "message4", createdAt = 4, NotificationMeta(false, Some("type3")))
        )
        /*
        List(NotificationListData(1,message1,1,NotificationMeta(false,None)),
        NotificationListData(3,message3,3,NotificationMeta(false,Some(type2))),
        NotificationListData(4,message4,4,NotificationMeta(false,Some(type3))))
         */
        notificationDao.findByUser("user_id2").unsafeRunSync() shouldEqual Seq(
          NotificationListData("2", "message2", createdAt = 2, NotificationMeta(false, Some("type1")))
        )
      }

    }
    "FeedbackDao" when {
      "#create" in {
        val data1 = CreateFeedbackData("user_1", "message1", 1)
        val data2 = CreateFeedbackData("user_2", "message2", 2)
        feedbackDao.create(data1).unsafeRunSync() shouldEqual "1"
        feedbackDao.create(data2).unsafeRunSync() shouldEqual "2"

        val result = await(
          feedbackCollection
            .find(BSONDocument(), None)
            .cursor[BSONDocument]()
            .collect[Seq](-1, Cursor.FailOnError[Seq[BSONDocument]]())
        )

        val resultMap = result.map(doc => doc.getId -> doc).toMap
        resultMap("1").get("userId") shouldEqual Some(BSONString("user_1"))
        resultMap("1").get("message") shouldEqual Some(BSONString("message1"))
        resultMap("1").get("createdAt") shouldEqual Some(BSONLong(1))
        resultMap("2").get("userId") shouldEqual Some(BSONString("user_2"))
        resultMap("2").get("message") shouldEqual Some(BSONString("message2"))
        resultMap("2").get("createdAt") shouldEqual Some(BSONLong(2))
      }
    }
  }
}
