package hu.ksisu.imazsak.core.dao

import hu.ksisu.imazsak.core.dao.BsonHelper._
import hu.ksisu.imazsak.core.dao.MongoSelectors._
import hu.ksisu.imazsak.core.dao.PrayerDao._
import hu.ksisu.imazsak.util.IdGenerator
import reactivemongo.api.Cursor
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.{ExecutionContext, Future}

class PrayerDaoImpl(
    implicit mongoDatabaseService: MongoDatabaseService[Future],
    idGenerator: IdGenerator,
    ec: ExecutionContext
) extends PrayerDao[Future] {
  protected val collectionF: Future[BSONCollection] = mongoDatabaseService.getCollection("prayers")

  override def createPrayer(data: CreatePrayerData): Future[String] = {
    for {
      collection <- collectionF
      model = data.toBsonWithNewId
      _ <- collection.insert(false).one(model)
    } yield model.getId
  }

  override def findPrayerByUser(userId: String): Future[Seq[MinePrayerListData]] = {
    for {
      collection <- collectionF
      prayers <- collection
        .find(byUserId(userId), minePrayerListDataProjector)
        .cursor[MinePrayerListData]()
        .collect[Seq](-1, Cursor.FailOnError[Seq[MinePrayerListData]]())
    } yield prayers
  }

  override def findByGroup(groupId: String): Future[Seq[GroupPrayerListData]] = {
    for {
      collection <- collectionF
      prayers <- collection
        .find(groupIdsContains(groupId), prayerListDataReaderProjector)
        .cursor[GroupPrayerListData]()
        .collect[Seq](-1, Cursor.FailOnError[Seq[GroupPrayerListData]]())
    } yield prayers
  }
}
