package hu.ksisu.imazsak.prayer

import hu.ksisu.imazsak.prayer.PrayerDao.{CreatePrayerData, GroupPrayerListData, MyPrayerListData}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros, document}

trait PrayerDao[F[_]] {
  def createPrayer(data: CreatePrayerData): F[String]
  def findPrayerByUser(userId: String): F[Seq[MyPrayerListData]]
  def findByGroup(groupId: String): F[Seq[GroupPrayerListData]]
  def incrementPrayCount(prayerId: String): F[Unit]
}

object PrayerDao {
  case class CreatePrayerData(userId: String, message: String, groupIds: Seq[String])
  case class MyPrayerListData(id: String, message: String, groupIds: Seq[String])
  case class GroupPrayerListData(id: String, userId: String, message: String)

  implicit val createPrayerDataWriter: BSONDocumentWriter[CreatePrayerData]       = Macros.writer[CreatePrayerData]
  implicit val myPrayerListDataReader: BSONDocumentReader[MyPrayerListData]       = Macros.reader[MyPrayerListData]
  implicit val groupPrayerListDataReader: BSONDocumentReader[GroupPrayerListData] = Macros.reader[GroupPrayerListData]

  val myPrayerListDataProjector: Option[BSONDocument]     = Option(document("id" -> 1, "groupIds" -> 1, "message" -> 1))
  val prayerListDataReaderProjector: Option[BSONDocument] = Option(document("id" -> 1, "userId"   -> 1, "message" -> 1))
}
