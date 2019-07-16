package hu.ksisu.imazsak.prayer

import cats.MonadError
import cats.data.EitherT
import hu.ksisu.imazsak.Errors.Response
import hu.ksisu.imazsak.core.dao.PrayerDao.{CreatePrayerData, MinePrayerListData}
import hu.ksisu.imazsak.core.dao.{GroupDao, PrayerDao}
import hu.ksisu.imazsak.prayer.PrayerService.CreatePrayerRequest
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

class PrayerServiceImpl[F[_]: MonadError[?[_], Throwable]](implicit prayerDao: PrayerDao[F], groupDao: GroupDao[F])
    extends PrayerService[F] {

  override def createPrayer(data: CreatePrayerRequest)(implicit ctx: UserLogContext): Response[F, Unit] = {
    val model = CreatePrayerData(ctx.userId, data.message, data.groupIds)
    for {
      _ <- checkMessage(data.message)
      _ <- checkGroups(data.groupIds)
      _ <- EitherT.right(prayerDao.createPrayer(model))
    } yield ()
  }

  override def listMyPrayers()(implicit ctx: UserLogContext): Response[F, Seq[MinePrayerListData]] = {
    EitherT.right(prayerDao.findPrayerByUser(ctx.userId))
  }

  private def checkGroups(groupIds: Seq[String])(implicit ctx: UserLogContext): Response[F, Unit] = {
    for {
      _      <- EitherT.cond(groupIds.nonEmpty, (), noGroupError)
      groups <- EitherT.right(groupDao.findGroupsByUser(ctx.userId))
      illegalGroups = groupIds.toSet -- groups.map(_.id).toSet
      _ <- EitherT.cond(illegalGroups.isEmpty, (), illegalGroupError(illegalGroups))
    } yield ()
  }

  private def checkMessage(message: String): Response[F, Unit] = {
    EitherT.cond(message.trim.length > 5, (), noMessageError)
  }

  private def noMessageError: Throwable = {
    new IllegalArgumentException("Must have add message to create a new prayer!")
  }

  private def noGroupError: Throwable = {
    new IllegalArgumentException("Must have at least one group to create a new prayer!")
  }

  private def illegalGroupError(groups: Set[String])(implicit ctx: UserLogContext): Throwable = {
    new IllegalAccessError(s"User ${ctx.userId} not member in: ${groups.mkString("[,", ",", "]")}")
  }
}
