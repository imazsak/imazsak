package hu.ksisu.imazsak.prayer

import cats.effect.{ContextShift, IO}
import hu.ksisu.imazsak.core.dao.MongoSelectors._
import hu.ksisu.imazsak.core.dao.{MongoDatabaseService, MongoQueryHelper}
import hu.ksisu.imazsak.prayer.PrayerDao._
import hu.ksisu.imazsak.util.IdGenerator
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.ExecutionContext

class PrayerDaoImpl(
    implicit mongoDatabaseService: MongoDatabaseService[IO],
    idGenerator: IdGenerator,
    ec: ExecutionContext,
    cs: ContextShift[IO]
) extends PrayerDao[IO] {
  protected implicit val collectionF: IO[BSONCollection] = mongoDatabaseService.getCollection("prayers")

  override def createPrayer(data: CreatePrayerData): IO[String] = {
    MongoQueryHelper.insert(data)
  }

  override def findPrayerByUser(userId: String): IO[Seq[MyPrayerListData]] = {
    MongoQueryHelper.list[MyPrayerListData](byUserId(userId), myPrayerListDataProjector)
  }

  override def findByGroup(groupId: String): IO[Seq[GroupPrayerListData]] = {
    MongoQueryHelper.list[GroupPrayerListData](groupIdsContains(groupId), prayerListDataReaderProjector)
  }
}
