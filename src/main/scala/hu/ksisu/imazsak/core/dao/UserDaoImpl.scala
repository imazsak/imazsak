package hu.ksisu.imazsak.core.dao

import cats.data.OptionT
import hu.ksisu.imazsak.core.dao.MongoSelectors._
import hu.ksisu.imazsak.core.dao.UserDao._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSON, document}

import scala.concurrent.{ExecutionContext, Future}

class UserDaoImpl(implicit mongoDatabaseService: MongoDatabaseService[Future], ec: ExecutionContext)
    extends UserDao[Future] {
  protected implicit val collectionF: Future[BSONCollection] = mongoDatabaseService.getCollection("users")

  override def findUserData(id: String): OptionT[Future, UserData] = {
    MongoQueryHelper.findOne[UserData](byId(id), userDataProjector)
  }

  override def updateUserData(userData: UserData): Future[Unit] = {
    val modifier = document("$set" -> BSON.writeDocument(userData).remove("id"))
    MongoQueryHelper.updateOne(byId(userData.id), modifier)
  }

  override def allUser(): Future[Seq[UserAdminListData]] = {
    MongoQueryHelper.list[UserAdminListData](all, userAdminListDataProjector)
  }
}
