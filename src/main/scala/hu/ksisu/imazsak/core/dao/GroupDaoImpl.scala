package hu.ksisu.imazsak.core.dao

import hu.ksisu.imazsak.core.dao.GroupDao._
import reactivemongo.api.Cursor
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.document

import scala.concurrent.{ExecutionContext, Future}

class GroupDaoImpl(implicit mongoDatabaseService: MongoDatabaseService[Future], ec: ExecutionContext)
    extends GroupDao[Future] {
  protected val collectionF: Future[BSONCollection] = mongoDatabaseService.getCollection("groups")

  override def findGroupsByUser(userId: String): Future[Seq[GroupListData]] = {
    val memberId = document("members.id" -> userId)
    for {
      collection <- collectionF
      userData <- collection
        .find(memberId, groupListDataProjector)
        .cursor[GroupListData]()
        .collect[Seq](-1, Cursor.FailOnError[Seq[GroupListData]]())
    } yield userData
  }
}
