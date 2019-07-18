package hu.ksisu.imazsak.prayer

import hu.ksisu.imazsak.core.dao.MongoSelectors._
import hu.ksisu.imazsak.core.dao.{MongoDatabaseService, MongoQueryHelper}
import hu.ksisu.imazsak.prayer.PrayerDao._
import hu.ksisu.imazsak.util.IdGenerator
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.{ExecutionContext, Future}

class PrayerDaoImpl(
    implicit mongoDatabaseService: MongoDatabaseService[Future],
    idGenerator: IdGenerator,
    ec: ExecutionContext
) extends PrayerDao[Future] {
  protected implicit val collectionF: Future[BSONCollection] = mongoDatabaseService.getCollection("prayers")

  override def createPrayer(data: CreatePrayerData): Future[String] = {
    MongoQueryHelper.insert(data)
  }

  override def findPrayerByUser(userId: String): Future[Seq[MinePrayerListData]] = {
    MongoQueryHelper.list[MinePrayerListData](byUserId(userId), minePrayerListDataProjector)
  }

  override def findByGroup(groupId: String): Future[Seq[GroupPrayerListData]] = {
    MongoQueryHelper.list[GroupPrayerListData](groupIdsContains(groupId), prayerListDataReaderProjector)
  }
}
