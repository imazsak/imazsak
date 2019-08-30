package hu.ksisu.imazsak.user

import hu.ksisu.imazsak.Errors.Response
import hu.ksisu.imazsak.user.UserDao.UserAdminListData
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

trait UserService[F[_]] {
  def listGroupUsers(groupId: String)(implicit ctx: UserLogContext): Response[F, Seq[UserAdminListData]]
}
