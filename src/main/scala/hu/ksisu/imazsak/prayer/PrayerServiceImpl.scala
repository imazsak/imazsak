package hu.ksisu.imazsak.prayer

import cats.MonadError
import hu.ksisu.imazsak.core.dao.PrayerDao.CreatePrayerData
import hu.ksisu.imazsak.core.dao.{GroupDao, PrayerDao}
import hu.ksisu.imazsak.prayer.PrayerService.CreatePrayerRequest
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

class PrayerServiceImpl[F[_]: MonadError[?[_], Throwable]](implicit prayerDao: PrayerDao[F], groupDao: GroupDao[F])
    extends PrayerService[F] {
  import cats.syntax.flatMap._
  import cats.syntax.functor._
  import hu.ksisu.imazsak.util.ApplicativeErrorSyntax._

  override def createPrayer(data: CreatePrayerRequest)(implicit ctx: UserLogContext): F[Unit] = {
    val model = CreatePrayerData(ctx.userId, data.message, data.groupIds)
    for {
      _ <- checkGroups(data.groupIds)
      _ <- prayerDao.createPrayer(model)
    } yield ()
  }

  private def checkGroups(groupIds: Seq[String])(implicit ctx: UserLogContext): F[Unit] = {
    lazy val noGroupError = new IllegalArgumentException("Must have at least one group to create a new prayer!")
    def illegalGroupError(groups: Set[String]) = {
      new IllegalAccessError(s"User ${ctx.userId} not member in: ${groups.mkString("[,", ",", "]")}")
    }
    for {
      _      <- groupIds.nonEmpty.pureUnitOrRise(noGroupError)
      groups <- groupDao.findGroupsByUser(ctx.userId)
      illegalGroups = groupIds.toSet -- groups.map(_.id).toSet
      _ <- illegalGroups.isEmpty.pureUnitOrRise(illegalGroupError(illegalGroups))
    } yield ()
  }
}
