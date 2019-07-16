package hu.ksisu.imazsak.core.dao

import hu.ksisu.imazsak.core.dao.PrayerDao.{CreatePrayerData, MinePrayerListData}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros, document}

trait PrayerDao[F[_]] {
  def createPrayer(data: CreatePrayerData): F[String]
  def findPrayerByUser(userId: String): F[Seq[MinePrayerListData]]
}

object PrayerDao {
  case class CreatePrayerData(userId: String, message: String, groupIds: Seq[String])
  case class MinePrayerListData(id: String, message: String, groupIds: Seq[String])

  implicit def createPrayerDataWriter: BSONDocumentWriter[CreatePrayerData]     = Macros.writer[CreatePrayerData]
  implicit def minePrayerListDataReader: BSONDocumentReader[MinePrayerListData] = Macros.reader[MinePrayerListData]

  val minePrayerListDataProjector: Option[BSONDocument] = Option(document("id" -> 1, "groupIds" -> 1, "message" -> 1))
}
