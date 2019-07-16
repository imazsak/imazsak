package hu.ksisu.imazsak.admin

import hu.ksisu.imazsak.Errors.Response
import hu.ksisu.imazsak.admin.AdminService.{AddUserToGroupRequest, CreateGroupRequest}
import hu.ksisu.imazsak.core.dao.GroupDao.GroupAdminListData
import hu.ksisu.imazsak.core.dao.UserDao.UserAdminListData
import hu.ksisu.imazsak.util.LoggerUtil.AdminLogContext

trait AdminService[F[_]] {
  def listAllGroup()(implicit ctx: AdminLogContext): Response[F, Seq[GroupAdminListData]]
  def listAllUser()(implicit ctx: AdminLogContext): Response[F, Seq[UserAdminListData]]
  def addUserToGroup(data: AddUserToGroupRequest)(implicit ctx: AdminLogContext): Response[F, Unit]
  def createGroup(data: CreateGroupRequest)(implicit ctx: AdminLogContext): Response[F, Unit]
}

object AdminService {
  case class CreateGroupRequest(name: String, adminUserId: String)
  case class AddUserToGroupRequest(userId: String, groupId: String)
}
