package hu.ksisu.imazsak.prayer

import cats.data.OptionT
import cats.effect.{ContextShift, IO}
import hu.ksisu.imazsak.core.dao.MongoSelectors._
import hu.ksisu.imazsak.core.dao.{MongoDatabaseService, MongoQueryHelper}
import hu.ksisu.imazsak.prayer.PrayerDao._
import hu.ksisu.imazsak.util.IdGenerator
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{BSONDocument, BSONNull, document}

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
    MongoQueryHelper.list[GroupPrayerListData](groupIdsContains(groupId), groupPrayerListDataProjector)
  }

  override def incrementPrayCount(userId: String, prayerId: String): IO[Unit] = {
    val incrementPrayCounter = document("$inc"      -> document("prayCount" -> 1))
    val addUserToPrayUsers   = document("$addToSet" -> document("prayUsers" -> userId))
    MongoQueryHelper.updateOne(byId(prayerId), incrementPrayCounter ++ addUserToPrayUsers)
  }

  override def findNextsByGroups(
      groupIds: Seq[String],
      excludedUserId: String,
      limit: Option[Int]
  ): IO[Seq[PrayerListData]] = {
    val selector      = document("groupIds"  -> document("$in" -> groupIds), "userId" -> document("$ne" -> excludedUserId))
    val prayIsNull    = document("prayCount" -> BSONNull)
    val prayIsNotNull = document("prayCount" -> document("$ne" -> BSONNull))
    val byPrayCount   = document("prayCount" -> 1)

    def firstPartQuery: IO[Seq[PrayerListData]] =
      MongoQueryHelper
        .list[PrayerListData](selector ++ prayIsNull, prayerListDataProjector, limit = limit)
    def secondPartQuery(limit: Option[Int]): IO[Seq[PrayerListData]] = {
      MongoQueryHelper
        .list[PrayerListData](selector ++ prayIsNotNull, prayerListDataProjector, Some(byPrayCount), limit)
    }
    def secondPartLimit(firstPartCount: Int): Option[Int] = limit.map {
      case oLimit if oLimit > firstPartCount => oLimit - firstPartCount
      case _                                 => 0
    }

    for {
      firstPart  <- firstPartQuery
      secondPart <- secondPartQuery(secondPartLimit(firstPart.size))
    } yield {
      val prayers = firstPart ++ secondPart
      val prayersWithFilteredGroups = prayers.map { prayer =>
        val searchedGroups = prayer.groupIds.filter(groupIds.contains)
        prayer.copy(groupIds = searchedGroups)
      }
      prayersWithFilteredGroups
    }
  }

  override def findById(prayerId: String): OptionT[IO, GroupPrayerListData] = {
    MongoQueryHelper.findOne[GroupPrayerListData](byId(prayerId), groupPrayerListDataProjector)
  }

  override def findWithPrayUserListById(prayerId: String): OptionT[IO, PrayerWithPrayUserData] = {
    MongoQueryHelper
      .findOne[BSONDocument](byId(prayerId), prayerWithPrayUserDataProjector)
      .subflatMap { doc =>
        println(doc.toMap)
        (document("prayUsers" -> Seq()) ++ doc).asOpt[PrayerWithPrayUserData]
      }
  }

  override def delete(prayerId: String): IO[Unit] = {
    MongoQueryHelper.deleteOne(byId(prayerId)).map(_ => ())
  }
}
