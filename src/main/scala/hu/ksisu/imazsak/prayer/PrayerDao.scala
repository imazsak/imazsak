package hu.ksisu.imazsak.prayer

import hu.ksisu.imazsak.prayer.PrayerDao.{CreatePrayerData, GroupPrayerListData, MyPrayerListData, PrayerListData}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros, document}

trait PrayerDao[F[_]] {
  def createPrayer(data: CreatePrayerData): F[String]
  def findPrayerByUser(userId: String): F[Seq[MyPrayerListData]]
  def findByGroup(groupId: String): F[Seq[GroupPrayerListData]]
  def incrementPrayCount(prayerId: String): F[Unit]
  def findByGroupIds(groupIds: Seq[String], limit: Option[Int] = None): F[Seq[PrayerListData]]
}

object PrayerDao {
  case class CreatePrayerData(userId: String, message: String, groupIds: Seq[String])
  case class MyPrayerListData(id: String, message: String, groupIds: Seq[String], prayCount: Option[Int])
  case class GroupPrayerListData(id: String, userId: String, message: String)
  case class PrayerListData(id: String, userId: String, groupIds: Seq[String], message: String)

  implicit val createPrayerDataWriter: BSONDocumentWriter[CreatePrayerData]       = Macros.writer[CreatePrayerData]
  implicit val myPrayerListDataReader: BSONDocumentReader[MyPrayerListData]       = Macros.reader[MyPrayerListData]
  implicit val groupPrayerListDataReader: BSONDocumentReader[GroupPrayerListData] = Macros.reader[GroupPrayerListData]
  implicit val prayerListDataReader: BSONDocumentReader[PrayerListData]           = Macros.reader[PrayerListData]

  val myPrayerListDataProjector: Option[BSONDocument] = Option(
    document("id" -> 1, "groupIds" -> 1, "message" -> 1, "prayCount" -> 1)
  )
  val groupPrayerListDataProjector: Option[BSONDocument] = Option(document("id" -> 1, "userId" -> 1, "message" -> 1))
  val prayerListDataProjector: Option[BSONDocument] = Option(
    document("id" -> 1, "userId" -> 1, "groupIds" -> 1, "message" -> 1)
  )
}
