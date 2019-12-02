package hu.ksisu.imazsak.user

import cats.data.OptionT
import cats.effect.{ContextShift, IO}
import hu.ksisu.imazsak.core.dao.MongoSelectors._
import hu.ksisu.imazsak.core.dao.{MongoDatabaseService, MongoQueryHelper}
import hu.ksisu.imazsak.user.UserDao._
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{BSON, BSONDocument, document}

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

  override def savePushSubscribe(id: String, data: UserPushSubscribeData): IO[Unit] = {
    val doc = document("push" -> (BSON.writeDocument(data).getOrElse(document())))
    MongoQueryHelper.updateOne(byId(id), document("$set" -> doc))
  }

  override def findPushSubscribe(id: String): OptionT[IO, UserPushSubscribeData] = {
    MongoQueryHelper.findOne[BSONDocument](byId(id), findPushSubscribeProjector).subflatMap { doc =>
      doc.getAsOpt[UserPushSubscribeData]("push")
    }
  }

  override def removePushSubscribe(id: String): IO[Unit] = {
    MongoQueryHelper.updateOne(byId(id), document("$unset" -> document("push" -> "")))
  }
}
