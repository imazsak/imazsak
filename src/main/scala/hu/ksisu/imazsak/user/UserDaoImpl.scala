package hu.ksisu.imazsak.user

import cats.data.OptionT
import cats.effect.{ContextShift, IO}
import hu.ksisu.imazsak.core.dao.MongoSelectors._
import hu.ksisu.imazsak.core.dao.{MongoDatabaseService, MongoQueryHelper}
import hu.ksisu.imazsak.user.UserDao._
import hu.ksisu.imazsak.user.UserDaoImpl.DeviceIdAndSubscription
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{BSON, BSONDocument, BSONDocumentHandler, Macros, document}

import scala.concurrent.ExecutionContext

class UserDaoImpl(implicit mongoDatabaseService: MongoDatabaseService[IO], ec: ExecutionContext, cs: ContextShift[IO])
    extends UserDao[IO] {
  protected implicit val collectionF: IO[BSONCollection] = mongoDatabaseService.getCollection("users")

  override def findUserData(id: String): OptionT[IO, UserData] = {
    MongoQueryHelper.findOne[UserData](byId(id), userDataProjector)
  }

  override def updateUserData(userData: UserData): IO[Unit] = {
    val modifier = document("$set" -> (BSON.writeDocument(userData).getOrElse(document()) -- "id"))
    MongoQueryHelper.updateOne(byId(userData.id), modifier)
  }

  override def allUser(): IO[Seq[UserAdminListData]] = {
    MongoQueryHelper.list[UserAdminListData](all, userAdminListDataProjector)
  }

  override def findUsersByIds(ids: Seq[String]): IO[Seq[UserAdminListData]] = {
    MongoQueryHelper.list[UserAdminListData](byIds(ids), userAdminListDataProjector)
  }

  override def isAdmin(id: String): IO[Boolean] = {
    MongoQueryHelper
      .findOne[BSONDocument](byId(id), isAdminProjector)
      .subflatMap(_.getAsOpt[Boolean]("isAdmin"))
      .getOrElse(false)
  }

  override def addPushSubscription(userId: String, deviceId: String, data: UserPushSubscriptionData): IO[Unit] = {
    val modifier = document("$push" -> document("push" -> DeviceIdAndSubscription(deviceId, data)))
    for {
      _ <- removePushSubscriptionByDeviceId(deviceId)
      _ <- MongoQueryHelper.updateOne(byId(userId), modifier)
    } yield ()
  }

  override def findPushSubscriptionsByUserId(userId: String): IO[Seq[UserPushSubscriptionData]] = {
    MongoQueryHelper
      .findOne[BSONDocument](byId(userId), findPushSubscriptionProjector)
      .subflatMap(_.getAsOpt[Seq[DeviceIdAndSubscription]]("push"))
      .map(_.map(_.sub))
      .getOrElse(Seq.empty)
  }

  override def removePushSubscriptionByDeviceId(deviceId: String): IO[Unit] = {
    val selector = document("push.deviceId" -> deviceId)
    val modifier = document("$pull"         -> document("push" -> document("deviceId" -> deviceId)))
    MongoQueryHelper.updateMultiple(selector, modifier)
  }
}

object UserDaoImpl {
  case class DeviceIdAndSubscription(deviceId: String, sub: UserPushSubscriptionData)
  implicit val deviceIdAndSubscriptionHandler: BSONDocumentHandler[DeviceIdAndSubscription] =
    Macros.handler[DeviceIdAndSubscription]
}
