package hu.ksisu.imazsak.notification

import cats.effect.{ContextShift, IO}
import hu.ksisu.imazsak.core.dao.MongoSelectors._
import hu.ksisu.imazsak.core.dao.{MongoDatabaseService, MongoQueryHelper}
import hu.ksisu.imazsak.notification.NotificationDao._
import hu.ksisu.imazsak.util.IdGenerator
import reactivemongo.api.ReadConcern
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{BSON, document}

import scala.concurrent.ExecutionContext

class NotificationDaoImpl(
    implicit mongoDatabaseService: MongoDatabaseService[IO],
    idGenerator: IdGenerator,
    ec: ExecutionContext,
    cs: ContextShift[IO]
) extends NotificationDao[IO] {
  protected implicit val collectionF: IO[BSONCollection] = mongoDatabaseService.getCollection("notifications")

  override def createNotification(data: CreateNotificationData): IO[String] = {
    MongoQueryHelper.insert(data)
  }

  override def findByUser(userId: String): IO[Seq[NotificationListData]] = {
    MongoQueryHelper.list[NotificationListData](byUserId(userId), notificationListDataProjector)
  }

  override def findByUserOrderByDateDesc(userId: String, limit: Option[Int]): IO[Seq[NotificationListData]] = {
    val order = document("createdAt" -> -1)
    MongoQueryHelper.sortedList[NotificationListData](byUserId(userId), order, notificationListDataProjector, limit)
  }

  override def updateMeta(id: String, meta: NotificationMeta): IO[Unit] = {
    val modifier = document("$set" -> document("meta" -> BSON.writeDocument(meta).getOrElse(document())))
    MongoQueryHelper.updateOne(byId(id), modifier)
  }

  override def deleteByIds(ids: Seq[String], userId: Option[String]): IO[Int] = {
    val selector = byIds(ids) ++ byOptionalUserId(userId)
    MongoQueryHelper.deleteMultiple(selector)
  }

  override def setRead(ids: Seq[String], userId: String): IO[Unit] = {
    val selector = byIds(ids) ++ byUserId(userId)
    val modifier = document("$set" -> document("meta.isRead" -> true))
    MongoQueryHelper.updateMultiple(selector, modifier)
  }

  override def countNotReadByUser(userId: String, limit: Option[Int]): IO[Long] = {
    val selector = byUserId(userId) ++ document("meta.isRead" -> false)
    for {
      collection <- collectionF
      count <- IO.fromFuture(
        IO(collection.count(Some(selector), limit, skip = 0, hint = None, readConcern = ReadConcern.Local))
      )
    } yield count
  }
}
