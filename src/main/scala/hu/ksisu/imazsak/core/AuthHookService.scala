package hu.ksisu.imazsak.core

trait AuthHookService[F[_]] {
  def verifyAuthService(token: String): F[Boolean]
  def isAdmin(id: String): F[Boolean]
}

object AuthHookService {
  case class AuthHookConfig(secret: String)
}
