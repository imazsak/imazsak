package hu.ksisu.imazsak.me

import cats.Monad
import hu.ksisu.imazsak.core.dao.UserDao
import hu.ksisu.imazsak.core.dao.UserDao.UserData
import hu.ksisu.imazsak.me.MeService.{MeUserData, UpdateMeUserData}
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

class MeServiceImpl[F[_]: Monad](implicit val userDao: UserDao[F]) extends MeService[F] {

  override def getUserData()(implicit ctx: UserLogContext): F[MeUserData] = {
    userDao
      .findUserData(ctx.userId)
      .map(x => MeUserData(x.name))
      .getOrElse(throw new Exception("User data not found!"))
  }

  override def updateUserData(data: UpdateMeUserData)(implicit ctx: UserLogContext): F[Unit] = {
    val userData = UserData(ctx.userId, Option(data.name))
    userDao.updateUserData(userData)
  }
}
