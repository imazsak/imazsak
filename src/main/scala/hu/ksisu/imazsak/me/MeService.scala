package hu.ksisu.imazsak.me

import hu.ksisu.imazsak.me.MeService.{MeUserData, UpdateMeUserData}
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

trait MeService[F[_]] {
  def getUserData()(implicit ctx: UserLogContext): F[MeUserData]
  def updateUserData(data: UpdateMeUserData)(implicit ctx: UserLogContext): F[Unit]
}

object MeService {
  case class MeUserData(name: Option[String])
  case class UpdateMeUserData(name: String)
}
