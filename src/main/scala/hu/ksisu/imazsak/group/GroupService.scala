package hu.ksisu.imazsak.group

import hu.ksisu.imazsak.core.dao.GroupDao.GroupListData
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

trait GroupService[F[_]] {
  def listGroups()(implicit ctx: UserLogContext): F[Seq[GroupListData]]
}
