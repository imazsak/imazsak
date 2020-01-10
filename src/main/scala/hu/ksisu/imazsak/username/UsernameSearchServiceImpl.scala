package hu.ksisu.imazsak.username
import cats.Monad
import cats.data.EitherT
import hu.ksisu.imazsak.Errors.Response
import hu.ksisu.imazsak.group.GroupDao
import hu.ksisu.imazsak.user.UserDao
import hu.ksisu.imazsak.user.UserDao.UserAdminListData
import hu.ksisu.imazsak.username.UsernameSearchService.UsernameData
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

class UsernameSearchServiceImpl[F[_]: Monad](implicit groupDao: GroupDao[F], userDao: UserDao[F])
    extends UsernameSearchService[F] {
  import cats.syntax.flatMap._
  import cats.syntax.functor._

  override def findUsernames(ids: Seq[String])(implicit ctx: UserLogContext): Response[F, Seq[UsernameData]] = {
    val accessibleIdsF = groupDao.findMembersByUserId(ctx.userId).map { acquaintance =>
      val acquaintanceIds = acquaintance.map(_.id)
      ids.filter(acquaintanceIds.contains)
    }

    val result = for {
      accessibleIds <- accessibleIdsF
      users         <- userDao.findUsersByIds(accessibleIds)
    } yield {
      users.collect {
        case UserAdminListData(id, Some(name)) if id != ctx.userId => UsernameData(id, name)
      }.distinct
    }

    EitherT.right(result)
  }
}
