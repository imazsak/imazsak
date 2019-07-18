package hu.ksisu.imazsak.group

import hu.ksisu.imazsak.group.GroupDao.GroupListData
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

trait GroupService[F[_]] {
  def listGroups()(implicit ctx: UserLogContext): F[Seq[GroupListData]]
}
