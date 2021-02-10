package hu.ksisu.imazsak.prayer

import cats.data.OptionT
import hu.ksisu.imazsak.prayer.PrayerDao.{
  CreatePrayerData,
  GroupPrayerListData,
  MyPrayerListData,
  PrayerDetailsData,
  PrayerListData,
  PrayerUpdateData,
  PrayerWithPrayUserData
}
import reactivemongo.api.bson._

import scala.util.Success

trait PrayerDao[F[_]] {
  def createPrayer(data: CreatePrayerData): F[String]
  def addUpdate(prayerId: String, data: PrayerUpdateData): F[Unit]
  def findByIdWithUpdates(prayerId: String): OptionT[F, PrayerDetailsData]
  def findPrayerByUser(userId: String): F[Seq[MyPrayerListData]]
  def findByGroup(groupId: String): F[Seq[GroupPrayerListData]]
  def incrementPrayCount(userId: String, prayerId: String): F[Unit]
  def findNextsByGroups(
      groupIds: Seq[String],
      excludedUserId: String,
      limit: Option[Int] = None
  ): F[Seq[PrayerListData]]
  def findById(prayerId: String): OptionT[F, GroupPrayerListData]
  def findWithPrayUserListById(prayerId: String): OptionT[F, PrayerWithPrayUserData]
  def delete(prayerId: String): F[Unit]
}

object PrayerDao {
  case class CreatePrayerData(userId: String, message: String, groupIds: Seq[String])
  case class PrayerUpdateData(message: String, createdAt: Long)
  case class PrayerDetailsData(
      id: String,
      userId: String,
      groupIds: Seq[String],
      message: String,
      createdAt: Long,
      updates: Seq[PrayerUpdateData]
  )
  case class MyPrayerListData(id: String, message: String, groupIds: Seq[String], prayCount: Int, createdAt: Long)
  case class GroupPrayerListData(id: String, userId: String, message: String)
  case class PrayerListData(id: String, userId: String, groupIds: Seq[String], message: String)
  case class PrayerWithPrayUserData(userId: String, message: String, prayUsers: Seq[String], groupIds: Seq[String])

  implicit val createPrayerDataWriter: BSONDocumentWriter[CreatePrayerData]       = Macros.writer[CreatePrayerData]
  implicit val prayerUpdateDataHandler: BSONDocumentHandler[PrayerUpdateData]     = Macros.handler[PrayerUpdateData]
  implicit val groupPrayerListDataReader: BSONDocumentReader[GroupPrayerListData] = Macros.reader[GroupPrayerListData]
  implicit val prayerListDataReader: BSONDocumentReader[PrayerListData]           = Macros.reader[PrayerListData]
  implicit val prayerWithPrayUserDataReader: BSONDocumentReader[PrayerWithPrayUserData] =
    Macros.reader[PrayerWithPrayUserData]

  implicit val myPrayerListDataReader: BSONDocumentReader[MyPrayerListData] =
    BSONDocumentReader.from((bson: BSONDocument) => {
      for {
        id            <- bson.getAsTry[String]("id")
        message       <- bson.getAsTry[String]("message")
        groupIdsArray <- bson.getAsTry[BSONArray]("groupIds")
        prayCount     <- bson.getAsTry[Int]("prayCount").orElse(Success(0))
        objectId      <- bson.getAsTry[BSONObjectID]("_id")
      } yield {
        val createdAtMillis = objectId.time
        val groupIds = groupIdsArray.values.collect { case BSONString(groupId) =>
          groupId
        }
        MyPrayerListData(id, message, groupIds, prayCount, createdAtMillis)
      }
    })

  implicit val prayerDetailsDataReader: BSONDocumentReader[PrayerDetailsData] =
    BSONDocumentReader.from((bson: BSONDocument) => {
      for {
        id            <- bson.getAsTry[String]("id")
        userId        <- bson.getAsTry[String]("userId")
        groupIdsArray <- bson.getAsTry[BSONArray]("groupIds")
        message       <- bson.getAsTry[String]("message")
        objectId      <- bson.getAsTry[BSONObjectID]("_id")
      } yield {
        val updates = bson.getAsOpt[Seq[PrayerUpdateData]]("updates").getOrElse(Seq.empty)
        val groupIds = groupIdsArray.values.collect { case BSONString(groupId) =>
          groupId
        }
        val createdAtMillis = objectId.time
        PrayerDetailsData(id, userId, groupIds, message, createdAtMillis, updates)
      }
    })

  val myPrayerListDataProjector: Option[BSONDocument] = Option(
    document("id" -> 1, "groupIds" -> 1, "message" -> 1, "prayCount" -> 1, "_id" -> 1)
  )
  val prayerDetailsDataProjector: Option[BSONDocument] = Option(
    document("id" -> 1, "userId" -> 1, "groupIds" -> 1, "message" -> 1, "updates" -> 1, "_id" -> 1)
  )
  val groupPrayerListDataProjector: Option[BSONDocument] = Option(document("id" -> 1, "userId" -> 1, "message" -> 1))
  val prayerListDataProjector: Option[BSONDocument] = Option(
    document("id" -> 1, "userId" -> 1, "groupIds" -> 1, "message" -> 1)
  )

  val prayerWithPrayUserDataProjector: Option[BSONDocument] = Option(
    document("userId" -> 1, "message" -> 1, "prayUsers" -> 1, "groupIds" -> 1)
  )
}
