package hu.ksisu.imazsak.core.impl

import cats.Applicative
import hu.ksisu.imazsak.core.AuthHookService
import hu.ksisu.imazsak.core.AuthHookService.AuthHookConfig
import hu.ksisu.imazsak.user.UserDao

class AuthHookServiceImpl[F[_]: Applicative](implicit config: AuthHookConfig, userDao: UserDao[F])
    extends AuthHookService[F] {
  import cats.syntax.applicative._

  override def isAdmin(id: String): F[Boolean] = {
    userDao.isAdmin(id)
  }

  override def verifyAuthService(token: String): F[Boolean] = {
    if (config.secret.isEmpty) {
      false.pure[F]
    } else {
      (config.secret == token).pure[F]
    }
  }
}
