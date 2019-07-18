package hu.ksisu.imazsak.user

import hu.ksisu.imazsak.Errors.Response
import hu.ksisu.imazsak.user.MeService.{MeUserData, UpdateMeUserData}
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

trait MeService[F[_]] {
  def getUserData()(implicit ctx: UserLogContext): Response[F, MeUserData]
  def updateUserData(data: UpdateMeUserData)(implicit ctx: UserLogContext): Response[F, Unit]
}

object MeService {
  case class MeUserData(name: Option[String])
  case class UpdateMeUserData(name: String)
}
