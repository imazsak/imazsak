package hu.ksisu.imazsak.core.dao

import hu.ksisu.imazsak.core.dao.PrayerDao.CreatePrayerData
import reactivemongo.bson.{BSONDocumentWriter, Macros}

trait PrayerDao[F[_]] {
  def createPrayer(data: CreatePrayerData): F[String]
}

object PrayerDao {
  case class CreatePrayerData(userId: String, message: String, groupIds: Seq[String])
  implicit def createPrayerDataWriter: BSONDocumentWriter[CreatePrayerData] = Macros.writer[CreatePrayerData]
}
