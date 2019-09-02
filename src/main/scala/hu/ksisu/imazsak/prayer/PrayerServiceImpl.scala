package hu.ksisu.imazsak.prayer

import cats.MonadError
import cats.data.EitherT
import hu.ksisu.imazsak.Errors.{AccessDeniedError, AppError, IllegalArgumentError, Response}
import hu.ksisu.imazsak.group.GroupDao
import hu.ksisu.imazsak.prayer.PrayerDao.{CreatePrayerData, GroupPrayerListData, MyPrayerListData}
import hu.ksisu.imazsak.prayer.PrayerService.{CreatePrayerRequest, Next10PrayerListData}
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

  override def listMyPrayers()(implicit ctx: UserLogContext): Response[F, Seq[MyPrayerListData]] = {
    EitherT
      .right(prayerDao.findPrayerByUser(ctx.userId))
      .map(_.map { prayer =>
        {
          prayer.copy(prayCount = prayer.prayCount.orElse(Some(0)))
        }
      })
  }

  override def listGroupPrayers(
      groupId: String
  )(implicit ctx: UserLogContext): Response[F, Seq[GroupPrayerListData]] = {
    for {
      _      <- checkGroups(Seq(groupId))
      result <- EitherT.right(prayerDao.findByGroup(groupId))
    } yield result
  }

  override def pray(groupId: String, prayerId: String)(implicit ctx: UserLogContext): Response[F, Unit] = {
    for {
      prayers <- listGroupPrayers(groupId)
      _       <- EitherT.cond(prayers.exists(_.id == prayerId), (), illegalAccessToPrayer(groupId, prayerId))
      _       <- EitherT.right(prayerDao.incrementPrayCount(prayerId))
    } yield ()
  }

  override def next10(groupIds: Seq[String])(implicit ctx: UserLogContext): Response[F, Seq[Next10PrayerListData]] = {
    for {
      _      <- checkGroups(groupIds)
      result <- EitherT.right(prayerDao.findByGroupIds(groupIds, Some(10)))
    } yield {
      result.flatMap { x =>
        x.groupIds.headOption.map(groupId => Next10PrayerListData(x.id, x.userId, groupId, x.message))
      }
    }
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

  private def noMessageError: AppError = {
    IllegalArgumentError("Must have add message to create a new prayer!")
  }

  private def noGroupError: AppError = {
    IllegalArgumentError("Must have at least one group to create a new prayer!")
  }

  private def illegalGroupError(groups: Set[String])(implicit ctx: UserLogContext): AppError = {
    AccessDeniedError(s"User ${ctx.userId} not member in: ${groups.mkString("[,", ",", "]")}")
  }

  private def illegalAccessToPrayer(groupId: String, prayerId: String): AppError = {
    AccessDeniedError(s"Prayer $prayerId not in the group: $groupId")
  }
}
