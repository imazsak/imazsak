package hu.ksisu.imazsak.group

import cats.Monad
import hu.ksisu.imazsak.group.GroupDao.GroupListData
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

class GroupServiceImpl[F[_]: Monad](implicit val groupDao: GroupDao[F]) extends GroupService[F] {
  override def listGroups()(implicit ctx: UserLogContext): F[Seq[GroupListData]] = {
    groupDao.findGroupsByUser(ctx.userId)
  }
}
