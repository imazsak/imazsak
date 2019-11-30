package hu.ksisu.imazsak.prayer

import cats.MonadError
import cats.data.EitherT
import hu.ksisu.imazsak.Errors.{AccessDeniedError, AppError, IllegalArgumentError, NotFoundError, Response}
import hu.ksisu.imazsak.group.GroupDao
import hu.ksisu.imazsak.notification.NotificationService
import hu.ksisu.imazsak.prayer.PrayerDao.{
  CreatePrayerData,
  GroupPrayerListData,
  MyPrayerListData,
  PrayerWithPrayUserData
}
import hu.ksisu.imazsak.prayer.PrayerService.{
  CreatePrayerRequest,
  Next10PrayerListData,
  PrayerCloseFeedbackNotificationData,
  PrayerCreatedNotificationData
}
import hu.ksisu.imazsak.user.UserDao
import hu.ksisu.imazsak.util.LoggerUtil.{Logger, UserLogContext}

class PrayerServiceImpl[F[_]: MonadError[?[_], Throwable]](
    implicit prayerDao: PrayerDao[F],
    groupDao: GroupDao[F],
    userDao: UserDao[F],
    notificationService: NotificationService[F]
) extends PrayerService[F] {
  import cats.syntax.applicativeError._
  import cats.syntax.functor._
  import cats.syntax.traverse._
  private type Tmp[T] = Response[F, T]

  private implicit val logger = new Logger("PrayerServiceImpl")

  override def createPrayer(data: CreatePrayerRequest)(implicit ctx: UserLogContext): Response[F, Unit] = {
    val model = CreatePrayerData(ctx.userId, data.message, data.groupIds)
    for {
      _  <- checkMessage(data.message)
      _  <- checkGroups(data.groupIds)
      id <- EitherT.right(prayerDao.createPrayer(model))
      _  <- sendNewPrayerNotificationWithoutError(id, data)
    } yield {}
  }

  private def sendNewPrayerNotificationWithoutError(id: String, data: CreatePrayerRequest)(
      implicit ctx: UserLogContext
  ): Response[F, Unit] = {
    val result = sendNewPrayerNotification(id, data)
      .recover {
        case error => logger.warn(s"sendNewPrayerNotification failed! $error")
      }
      .value
      .recover {
        case ex =>
          logger.warn(s"sendNewPrayerNotification failed!", ex)
          Right({})
      }
    EitherT(result)
  }

  override def listMyPrayers()(implicit ctx: UserLogContext): Response[F, Seq[MyPrayerListData]] = {
    EitherT.right(prayerDao.findPrayerByUser(ctx.userId))
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
      _       <- EitherT.right(prayerDao.incrementPrayCount(ctx.userId, prayerId))
    } yield ()
  }

  override def next10(groupIds: Seq[String])(implicit ctx: UserLogContext): Response[F, Seq[Next10PrayerListData]] = {
    for {
      _      <- checkGroups(groupIds)
      result <- EitherT.right(prayerDao.findNextsByGroups(groupIds, ctx.userId, Some(10)))
    } yield {
      result.flatMap { x =>
        x.groupIds.headOption.map(groupId => Next10PrayerListData(x.id, x.userId, groupId, x.message))
      }
    }
  }

  override def close(data: PrayerService.PrayerCloseRequest)(implicit ctx: UserLogContext): Response[F, Unit] = {
    for {
      prayerData <- loadPrayerAndCheckPrayerBelongsToCurrentUser(data.id)
      _          <- sendFeedbackToUsers(prayerData, data.message.getOrElse(""))
      _          <- EitherT.right(prayerDao.delete(data.id))
    } yield ()
  }

  private def checkGroups(groupIds: Seq[String])(implicit ctx: UserLogContext): Response[F, Unit] = {
    for {
      _      <- EitherT.cond(groupIds.nonEmpty, (), noGroupError)
      groups <- EitherT.right(groupDao.findGroupsByUser(ctx.userId))
      illegalGroups = groupIds.toSet -- groups.map(_.id).toSet
      _ <- EitherT.cond(illegalGroups.isEmpty, (), illegalGroupError(illegalGroups))
    } yield ()
  }

  // TODO: refactor the whole function
  private def sendNewPrayerNotification(id: String, data: CreatePrayerRequest)(
      implicit ctx: UserLogContext
  ): Response[F, Unit] = {
    import cats.instances.list._
    val usersWithGroupIds = data.groupIds.toList
      .flatTraverse(
        id =>
          groupDao
            .findMembersByGroupId(id)
            .map(_.map(_.id -> id).toList)
      )
      .map(_.groupMap(_._1)(_._2))
      .map(_.filter(_._1 != ctx.userId).toList)

    EitherT
      .right(usersWithGroupIds)
      .flatMap(_.traverse[Tmp, Unit] {
        case (userId, groupIds) =>
          createPrayerCreatedNotificationData(userId, data.message, groupIds).flatMap { msg =>
            notificationService.createNotification("PRAYER_CREATED", userId, msg)
          }
      })
      .map(_ => {})
  }

  private def createPrayerCreatedNotificationData(id: String, message: String, groupIds: Seq[String])(
      implicit ctx: UserLogContext
  ): Response[F, PrayerCreatedNotificationData] = {
    val userNameFO = userDao
      .findUserData(ctx.userId)
      .map(_.name)
      .getOrElse(None)
    val result = userNameFO.map { userNameO =>
      PrayerCreatedNotificationData(id, userNameO, message, groupIds)
    }
    EitherT.right(result)
  }

  private def sendFeedbackToUsers(prayerData: PrayerWithPrayUserData, feedback: String)(
      implicit ctx: UserLogContext
  ): Response[F, Unit] = {
    if (feedback.trim.isEmpty) {
      EitherT.rightT({})
    } else {
      import cats.instances.list._
      createPrayerCloseFeedbackNotificationData(prayerData, feedback).flatMap { msg =>
        prayerData.prayUsers.toList
          .traverse[Tmp, Unit](userId => notificationService.createNotification("PRAYER_CLOSE_FEEDBACK", userId, msg))
          .map(_ => {})
      }
    }
  }

  private def createPrayerCloseFeedbackNotificationData(prayerData: PrayerWithPrayUserData, feedback: String)(
      implicit ctx: UserLogContext
  ): Response[F, PrayerCloseFeedbackNotificationData] = {
    val userNameFO = userDao
      .findUserData(ctx.userId)
      .map(_.name)
      .getOrElse(None)
    val result = userNameFO.map { userNameO =>
      PrayerCloseFeedbackNotificationData(userNameO, prayerData.message, feedback)
    }
    EitherT.right(result)
  }

  private def loadPrayerAndCheckPrayerBelongsToCurrentUser(
      prayerId: String
  )(implicit ctx: UserLogContext): Response[F, PrayerWithPrayUserData] = {
    prayerDao
      .findWithPrayUserListById(prayerId)
      .toRight(notFound(prayerId))
      .ensure(notTheCurrentUsersPrayer(prayerId))(_.userId == ctx.userId)
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

  private def notFound(prayerId: String): AppError = {
    NotFoundError(s"Prayer ${prayerId}not found")
  }

  private def notTheCurrentUsersPrayer(prayerId: String)(implicit ctx: UserLogContext): AppError = {
    AccessDeniedError(s"Prayer $prayerId not belongs to user ${ctx.userId}")
  }
}
