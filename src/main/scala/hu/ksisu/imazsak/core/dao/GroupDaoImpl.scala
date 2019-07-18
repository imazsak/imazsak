package hu.ksisu.imazsak.core.dao

import cats.data.OptionT
import hu.ksisu.imazsak.core.dao.GroupDao._
import hu.ksisu.imazsak.core.dao.MongoProjectors._
import hu.ksisu.imazsak.core.dao.MongoSelectors._
import hu.ksisu.imazsak.util.IdGenerator
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSON, BSONDocument, document}

import scala.concurrent.{ExecutionContext, Future}

class GroupDaoImpl(
    implicit mongoDatabaseService: MongoDatabaseService[Future],
    idGenerator: IdGenerator,
    ec: ExecutionContext
) extends GroupDao[Future] {
  import cats.instances.future._

  protected implicit val collectionF: Future[BSONCollection] = mongoDatabaseService.getCollection("groups")

  override def findGroupsByUser(userId: String): Future[Seq[GroupListData]] = {
    MongoQueryHelper.list[GroupListData](memberIdsContains(userId), groupListDataProjector)
  }

  override def findGroupByName(name: String): OptionT[Future, GroupListData] = {
    MongoQueryHelper.findOne[GroupListData](byName(name), groupListDataProjector)
  }

  override def allGroup(): Future[Seq[GroupAdminListData]] = {
    MongoQueryHelper.list[GroupAdminListData](all, groupAdminListDataProjector)
  }

  override def isMember(groupId: String, userId: String): Future[Boolean] = {
    MongoQueryHelper.findOne[BSONDocument](byId(groupId) ++ memberIdsContains(userId), existsProjector).isDefined
  }

  override def addMemberToGroup(groupId: String, member: GroupMember): Future[Unit] = {
    val modifier = document("$push" -> document("members" -> BSON.write(member)))
    MongoQueryHelper.updateOne(byId(groupId), modifier)
  }

  override def createGroup(data: CreateGroupData): Future[String] = {
    MongoQueryHelper.insert(data)
  }
}
