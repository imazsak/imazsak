package hu.ksisu.imazsak.core.dao

import hu.ksisu.imazsak.core.dao.MongoSelectors._
import hu.ksisu.imazsak.core.dao.NotificationDao._
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

  override def updateMeta(id: String, meta: NotificationMeta): Future[Unit] = {
    val modifier = document("$set" -> document("meta" -> BSON.writeDocument(meta)))
    MongoQueryHelper.updateOne(byId(id), modifier)
  }

  override def delete(id: String): Future[Unit] = {
    MongoQueryHelper.deleteOne(byId(id))
  }
}
