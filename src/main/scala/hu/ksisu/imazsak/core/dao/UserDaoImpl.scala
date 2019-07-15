package hu.ksisu.imazsak.core.dao

import cats.data.OptionT
import hu.ksisu.imazsak.core.dao.MongoSelectors._
import hu.ksisu.imazsak.core.dao.UserDao._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSON, BSONDocument}

import scala.concurrent.{ExecutionContext, Future}

class UserDaoImpl(implicit mongoDatabaseService: MongoDatabaseService[Future], ec: ExecutionContext)
    extends UserDao[Future] {
  protected val collectionF: Future[BSONCollection] = mongoDatabaseService.getCollection("users")

  override def findUserData(id: String): OptionT[Future, UserData] = {
    OptionT(for {
      collection <- collectionF
      userData   <- collection.find(byId(id), userDataProjector).one[UserData]
    } yield userData)
  }

  override def updateUserData(userData: UserData): Future[Unit] = {
    val modifier = BSONDocument(
      "$set" -> BSON.writeDocument(userData).remove("id")
    )
    for {
      collection <- collectionF
      _          <- collection.update.one(byId(userData.id), modifier, upsert = false, multi = false)
    } yield ()
  }
}
