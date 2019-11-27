package hu.ksisu.imazsak.group

import cats.data.OptionT
import hu.ksisu.imazsak.group.GroupDao.{CreateGroupData, GroupAdminListData, GroupListData, GroupMember}
import reactivemongo.api.bson.{
  BSONDocument,
  BSONDocumentHandler,
  BSONDocumentReader,
  BSONDocumentWriter,
  Macros,
  document
}

trait GroupDao[F[_]] {
  def findGroupsByUser(userId: String): F[Seq[GroupListData]]
  def findGroupByName(name: String): OptionT[F, GroupListData]
  def findMembersByGroupId(groupId: String): F[Seq[GroupMember]]
  def isMember(groupId: String, userId: String): F[Boolean]
  def addMemberToGroup(groupId: String, member: GroupMember): F[Unit]
  def allGroup(): F[Seq[GroupAdminListData]]
  def createGroup(data: CreateGroupData): F[String]
}

object GroupDao {
  case class GroupListData(id: String, name: String)
  case class GroupMember(id: String)
  case class GroupAdminListData(id: String, name: String, members: Seq[GroupMember])
  case class GroupMemberListData(members: Seq[GroupMember])
  case class CreateGroupData(name: String, members: Seq[GroupMember])

  implicit val groupListDataReader: BSONDocumentReader[GroupListData]             = Macros.reader[GroupListData]
  implicit val groupMemberHandler: BSONDocumentHandler[GroupMember]               = Macros.handler[GroupMember]
  implicit val groupAdminListDataHandler: BSONDocumentHandler[GroupAdminListData] = Macros.handler[GroupAdminListData]
  implicit val groupMemberListDataReader: BSONDocumentReader[GroupMemberListData] = Macros.reader[GroupMemberListData]
  implicit val createGroupDataWriter: BSONDocumentWriter[CreateGroupData]         = Macros.writer[CreateGroupData]

  val groupListDataProjector: Option[BSONDocument]       = Option(document("id"      -> 1, "name" -> 1))
  val groupAdminListDataProjector: Option[BSONDocument]  = Option(document("id"      -> 1, "name" -> 1, "members" -> 1))
  val groupMemberListDataProjector: Option[BSONDocument] = Option(document("members" -> 1))
}
