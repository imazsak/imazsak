package hu.ksisu.imazsak.username

import hu.ksisu.imazsak.Errors.Response
import hu.ksisu.imazsak.username.UsernameSearchService.UsernameData
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

trait UsernameSearchService[F[_]] {
  def findUsernames(ids: Seq[String])(implicit ctx: UserLogContext): Response[F, Seq[UsernameData]]
}

object UsernameSearchService {
  case class UsernameData(id: String, username: String)
}
