package hu.ksisu.imazsak.core.dao

import hu.ksisu.imazsak.core.dao.BsonHelper._
import hu.ksisu.imazsak.core.dao.GroupDao._
import hu.ksisu.imazsak.core.dao.MongoProjectors._
import hu.ksisu.imazsak.core.dao.MongoSelectors._
import hu.ksisu.imazsak.util.IdGenerator
import reactivemongo.api.Cursor
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSON, BSONDocument, document}

import scala.concurrent.{ExecutionContext, Future}

class GroupDaoImpl(
    implicit mongoDatabaseService: MongoDatabaseService[Future],
    idGenerator: IdGenerator,
    ec: ExecutionContext
) extends GroupDao[Future] {
  protected val collectionF: Future[BSONCollection] = mongoDatabaseService.getCollection("groups")

  override def findGroupsByUser(userId: String): Future[Seq[GroupListData]] = {
    for {
      collection <- collectionF
      groups <- collection
        .find(memberIdsContains(userId), groupListDataProjector)
        .cursor[GroupListData]()
        .collect[Seq](-1, Cursor.FailOnError[Seq[GroupListData]]())
    } yield groups
  }

  override def findGroupByName(name: String): Future[Option[GroupListData]] = {
    for {
      collection <- collectionF
      groups <- collection
        .find(byName(name), groupListDataProjector)
        .one[GroupListData]
    } yield groups
  }

  override def allGroup(): Future[Seq[GroupAdminListData]] = {
    for {
      collection <- collectionF
      groups <- collection
        .find(all, groupAdminListDataProjector)
        .cursor[GroupAdminListData]()
        .collect[Seq](-1, Cursor.FailOnError[Seq[GroupAdminListData]]())
    } yield groups
  }

  override def isMember(groupId: String, userId: String): Future[Boolean] = {
    for {
      collection <- collectionF
      group <- collection
        .find(byId(groupId) ++ memberIdsContains(userId), existsProjector)
        .one[BSONDocument]
    } yield group.isDefined
  }

  override def addMemberToGroup(groupId: String, member: GroupMember): Future[Unit] = {
    val modifier = document("$push" -> document("members" -> BSON.write(member)))
    for {
      collection <- collectionF
      _          <- collection.update.one(byId(groupId), modifier)
    } yield ()
  }

  override def createGroup(data: CreateGroupData): Future[String] = {
    for {
      collection <- collectionF
      model = data.toBsonWithNewId
      _ <- collection.insert(false).one(model)
    } yield model.getId
  }
}
