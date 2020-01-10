package hu.ksisu.imazsak.core.dao

import cats.effect.IO
import hu.ksisu.imazsak.AwaitUtil
import hu.ksisu.imazsak.notification.NotificationDao
import hu.ksisu.imazsak.notification.NotificationDao.{CreateNotificationData, NotificationListData, NotificationMeta}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import reactivemongo.api.bson.collection.BSONCollection

trait NotificationDaoSpec {
  self: AnyWordSpecLike with Matchers with AwaitUtil =>

  protected val notificationDao: NotificationDao[IO]
  protected val notificationCollection: BSONCollection

  def notificationDaoTests(): Unit = {

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
      "#setRead #countNotReadByUser" in {
        val userId1 = "user_id1"
        val userId2 = "user_id2"

        notificationDao.countNotReadByUser(userId1).unsafeRunSync() shouldEqual 0L

        val data1 = CreateNotificationData(userId1, "message1", createdAt = 1, NotificationMeta(false, None))
        val data2 = CreateNotificationData(userId2, "message2", createdAt = 2, NotificationMeta(false, Some("type1")))
        val data3 = CreateNotificationData(userId1, "message3", createdAt = 3, NotificationMeta(false, Some("type2")))
        val data4 = CreateNotificationData(userId1, "message4", createdAt = 4, NotificationMeta(false, Some("type3")))
        notificationDao.createNotification(data1).unsafeRunSync() shouldEqual "1"
        notificationDao.createNotification(data2).unsafeRunSync() shouldEqual "2"
        notificationDao.createNotification(data3).unsafeRunSync() shouldEqual "3"
        notificationDao.createNotification(data4).unsafeRunSync() shouldEqual "4"

        notificationDao.countNotReadByUser(userId1).unsafeRunSync() shouldEqual 3L
        notificationDao.countNotReadByUser(userId1, Some(2)).unsafeRunSync() shouldEqual 2L
        notificationDao.countNotReadByUser(userId2).unsafeRunSync() shouldEqual 1L

        notificationDao.setRead(Seq("1", "3"), userId1).unsafeRunSync()
        notificationDao.findByUser(userId1).unsafeRunSync() shouldEqual Seq(
          NotificationListData("1", "message1", createdAt = 1, NotificationMeta(true, None)),
          NotificationListData("3", "message3", createdAt = 3, NotificationMeta(true, Some("type2"))),
          NotificationListData("4", "message4", createdAt = 4, NotificationMeta(false, Some("type3")))
        )
        notificationDao.findByUser(userId2).unsafeRunSync() shouldEqual Seq(
          NotificationListData("2", "message2", createdAt = 2, NotificationMeta(false, Some("type1")))
        )
        notificationDao.countNotReadByUser(userId1).unsafeRunSync() shouldEqual 1L
      }

    }
  }
}
