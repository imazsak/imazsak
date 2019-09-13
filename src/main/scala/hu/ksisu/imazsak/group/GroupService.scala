package hu.ksisu.imazsak.group

import hu.ksisu.imazsak.Errors.Response
import hu.ksisu.imazsak.group.GroupDao.GroupListData
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

trait GroupService[F[_]] {
  def listGroups()(implicit ctx: UserLogContext): Response[F, Seq[GroupListData]]
  def createJoinToken(groupId: String)(implicit ctx: UserLogContext): Response[F, String]
  def joinToGroup(groupId: String, token: String)(implicit ctx: UserLogContext): Response[F, Unit]
}
