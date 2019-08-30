package hu.ksisu.imazsak.user

import cats.Monad
import cats.data.EitherT
import hu.ksisu.imazsak.Errors.{AccessDeniedError, AppError, Response}
import hu.ksisu.imazsak.group.GroupDao
import hu.ksisu.imazsak.user.UserDao.UserAdminListData
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

class UserServiceImpl[F[_]: Monad](implicit userDao: UserDao[F], groupDao: GroupDao[F]) extends UserService[F] {

  override def listGroupUsers(groupId: String)(implicit ctx: UserLogContext): Response[F, Seq[UserAdminListData]] = {
    for {
      memberIds <- EitherT
        .right[AppError](groupDao.findMembersByGroupId(groupId))
        .map(_.map(_.id))
        .ensure(illegalGroupError(groupId))(_.contains(ctx.userId))
      users <- EitherT.right[AppError](userDao.findUsersByIds(memberIds))
    } yield users
  }

  private def illegalGroupError(groupId: String)(implicit ctx: UserLogContext): AppError = {
    AccessDeniedError(s"User ${ctx.userId} not member in: $groupId}")
  }
}
