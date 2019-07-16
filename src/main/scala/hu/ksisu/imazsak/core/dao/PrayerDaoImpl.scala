package hu.ksisu.imazsak.core.dao

import hu.ksisu.imazsak.core.dao.BsonHelper._
import hu.ksisu.imazsak.core.dao.PrayerDao.CreatePrayerData
import hu.ksisu.imazsak.util.IdGenerator
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
}
