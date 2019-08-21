package hu.ksisu.imazsak.group

import cats.data.OptionT
import cats.effect.{ContextShift, IO}
import hu.ksisu.imazsak.core.dao.MongoProjectors._
import hu.ksisu.imazsak.core.dao.MongoSelectors._
import hu.ksisu.imazsak.core.dao.{MongoDatabaseService, MongoQueryHelper}
import hu.ksisu.imazsak.group.GroupDao._
import hu.ksisu.imazsak.util.IdGenerator
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSON, BSONDocument, document}

import scala.concurrent.ExecutionContext

class GroupDaoImpl(
    implicit mongoDatabaseService: MongoDatabaseService[IO],
    idGenerator: IdGenerator,
    ec: ExecutionContext,
    cs: ContextShift[IO]
) extends GroupDao[IO] {

  protected implicit val collectionF: IO[BSONCollection] = mongoDatabaseService.getCollection("groups")

  override def findGroupsByUser(userId: String): IO[Seq[GroupListData]] = {
    MongoQueryHelper.list[GroupListData](memberIdsContains(userId), groupListDataProjector)
  }

  override def findGroupByName(name: String): OptionT[IO, GroupListData] = {
    MongoQueryHelper.findOne[GroupListData](byName(name), groupListDataProjector)
  }

  override def allGroup(): IO[Seq[GroupAdminListData]] = {
    MongoQueryHelper.list[GroupAdminListData](all, groupAdminListDataProjector)
  }

  override def isMember(groupId: String, userId: String): IO[Boolean] = {
    MongoQueryHelper.findOne[BSONDocument](byId(groupId) ++ memberIdsContains(userId), existsProjector).isDefined
  }

  override def addMemberToGroup(groupId: String, member: GroupMember): IO[Unit] = {
    val modifier = document("$push" -> document("members" -> BSON.write(member)))
    MongoQueryHelper.updateOne(byId(groupId), modifier)
  }

  override def createGroup(data: CreateGroupData): IO[String] = {
    MongoQueryHelper.insert(data)
  }
}
