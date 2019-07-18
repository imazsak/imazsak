package hu.ksisu.imazsak.core.dao

import cats.data.OptionT
import hu.ksisu.imazsak.core.dao.GroupDao.{CreateGroupData, GroupAdminListData, GroupListData, GroupMember}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros, document}

trait GroupDao[F[_]] {
  def findGroupsByUser(userId: String): F[Seq[GroupListData]]
  def findGroupByName(name: String): OptionT[F, GroupListData]
  def isMember(groupId: String, userId: String): F[Boolean]
  def addMemberToGroup(groupId: String, member: GroupMember): F[Unit]
  def allGroup(): F[Seq[GroupAdminListData]]
  def createGroup(data: CreateGroupData): F[String]
}

object GroupDao {
  case class GroupListData(id: String, name: String)
  case class GroupMember(id: String)
  case class GroupAdminListData(id: String, name: String, members: Seq[GroupMember])
  case class CreateGroupData(name: String, members: Seq[GroupMember])

  implicit def groupListDataReader: BSONDocumentReader[GroupListData]           = Macros.reader[GroupListData]
  implicit def groupMemberReader: BSONDocumentReader[GroupMember]               = Macros.reader[GroupMember]
  implicit def groupMemberWriter: BSONDocumentWriter[GroupMember]               = Macros.writer[GroupMember]
  implicit def groupAdminListDataReader: BSONDocumentReader[GroupAdminListData] = Macros.reader[GroupAdminListData]
  implicit def groupAdminListDataWriter: BSONDocumentWriter[GroupAdminListData] = Macros.writer[GroupAdminListData]
  implicit def createGroupDataWriter: BSONDocumentWriter[CreateGroupData]       = Macros.writer[CreateGroupData]

  val groupListDataProjector: Option[BSONDocument]      = Option(document("id" -> 1, "name" -> 1))
  val groupAdminListDataProjector: Option[BSONDocument] = Option(document("id" -> 1, "name" -> 1, "members" -> 1))
}
