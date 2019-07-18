package hu.ksisu.imazsak.prayer

import hu.ksisu.imazsak.prayer.PrayerDao.{CreatePrayerData, GroupPrayerListData, MinePrayerListData}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros, document}

trait PrayerDao[F[_]] {
  def createPrayer(data: CreatePrayerData): F[String]
  def findPrayerByUser(userId: String): F[Seq[MinePrayerListData]]
  def findByGroup(groupId: String): F[Seq[GroupPrayerListData]]
}

object PrayerDao {
  case class CreatePrayerData(userId: String, message: String, groupIds: Seq[String])
  case class MinePrayerListData(id: String, message: String, groupIds: Seq[String])
  case class GroupPrayerListData(id: String, userId: String, message: String)

  implicit def createPrayerDataWriter: BSONDocumentWriter[CreatePrayerData]       = Macros.writer[CreatePrayerData]
  implicit def minePrayerListDataReader: BSONDocumentReader[MinePrayerListData]   = Macros.reader[MinePrayerListData]
  implicit def groupPrayerListDataReader: BSONDocumentReader[GroupPrayerListData] = Macros.reader[GroupPrayerListData]

  val minePrayerListDataProjector: Option[BSONDocument]   = Option(document("id" -> 1, "groupIds" -> 1, "message" -> 1))
  val prayerListDataReaderProjector: Option[BSONDocument] = Option(document("id" -> 1, "userId"   -> 1, "message" -> 1))
}
