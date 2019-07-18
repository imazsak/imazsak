package hu.ksisu.imazsak.notification

import hu.ksisu.imazsak.core.dao.MongoSelectors.{byOptionalUserId, _}
import hu.ksisu.imazsak.core.dao.{MongoDatabaseService, MongoQueryHelper}
import hu.ksisu.imazsak.notification.NotificationDao._
import hu.ksisu.imazsak.util.IdGenerator
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSON, document}

import scala.concurrent.{ExecutionContext, Future}

class NotificationDaoImpl(
    implicit mongoDatabaseService: MongoDatabaseService[Future],
    idGenerator: IdGenerator,
    ec: ExecutionContext
) extends NotificationDao[Future] {
  protected implicit val collectionF: Future[BSONCollection] = mongoDatabaseService.getCollection("notifications")

  override def createNotification(data: CreateNotificationData): Future[String] = {
    MongoQueryHelper.insert(data)
  }

  override def findByUser(userId: String): Future[Seq[NotificationListData]] = {
    MongoQueryHelper.list[NotificationListData](byUserId(userId), notificationListDataProjector)
  }

  override def findByUserOrderByDateDesc(userId: String, limit: Option[Int]): Future[Seq[NotificationListData]] = {
    val order = document("createdAt" -> -1)
    MongoQueryHelper.sortedList[NotificationListData](byUserId(userId), order, notificationListDataProjector, limit)
  }

  override def updateMeta(id: String, meta: NotificationMeta): Future[Unit] = {
    val modifier = document("$set" -> document("meta" -> BSON.writeDocument(meta)))
    MongoQueryHelper.updateOne(byId(id), modifier)
  }

  override def deleteByIds(ids: Seq[String], userId: Option[String]): Future[Int] = {
    val selector = byIds(ids) ++ byOptionalUserId(userId)
    MongoQueryHelper.deleteMultiple(selector)
  }

  override def setRead(ids: Seq[String], userId: String): Future[Unit] = {
    val selector = byIds(ids) ++ byUserId(userId)
    val modifier = document("$set" -> document("meta.isRead" -> true))
    MongoQueryHelper.updateMultiple(selector, modifier)
  }
}
