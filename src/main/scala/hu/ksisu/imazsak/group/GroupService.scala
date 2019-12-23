package hu.ksisu.imazsak.group

import hu.ksisu.imazsak.Errors.Response
import hu.ksisu.imazsak.group.GroupDao.GroupListData
import hu.ksisu.imazsak.group.GroupService.GroupUserListData
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

trait GroupService[F[_]] {
  def listGroups()(implicit ctx: UserLogContext): Response[F, Seq[GroupListData]]
  def listGroupUsers(groupId: String)(implicit ctx: UserLogContext): Response[F, Seq[GroupUserListData]]
  def createJoinToken(groupId: String)(implicit ctx: UserLogContext): Response[F, String]
  def joinToGroup(token: String)(implicit ctx: UserLogContext): Response[F, Unit]
  def checkGroups(groupIds: Seq[String])(implicit ctx: UserLogContext): Response[F, Unit]
}

object GroupService {
  case class GroupUserListData(id: String, name: Option[String])
}
