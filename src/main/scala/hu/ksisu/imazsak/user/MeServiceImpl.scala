package hu.ksisu.imazsak.user

import cats.Monad
import cats.data.EitherT
import hu.ksisu.imazsak.Errors.{AppError, NotFoundError, Response}
import hu.ksisu.imazsak.user.MeService.{MeUserData, UpdateMeUserData}
import hu.ksisu.imazsak.user.UserDao.UserData
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

class MeServiceImpl[F[_]: Monad](implicit val userDao: UserDao[F]) extends MeService[F] {

  override def getUserData()(implicit ctx: UserLogContext): Response[F, MeUserData] = {
    userDao
      .findUserData(ctx.userId)
      .map(x => MeUserData(x.name))
      .toRight(userDataNotFound)
  }

  override def updateUserData(data: UpdateMeUserData)(implicit ctx: UserLogContext): Response[F, Unit] = {
    val userData = UserData(ctx.userId, Option(data.name))
    EitherT.right(userDao.updateUserData(userData))
  }

  private def userDataNotFound(implicit ctx: UserLogContext): AppError = {
    NotFoundError(s"Not found user data for: ${ctx.userId}")
  }
}
