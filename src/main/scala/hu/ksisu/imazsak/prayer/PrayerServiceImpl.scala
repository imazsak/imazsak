package hu.ksisu.imazsak.prayer

import cats.MonadError
import cats.data.EitherT
import hu.ksisu.imazsak.Errors.{AccessDeniedError, AppError, IllegalArgumentError, NotFoundError, Response}
import hu.ksisu.imazsak.core.CacheService
import hu.ksisu.imazsak.group.{GroupDao, GroupService}
import hu.ksisu.imazsak.notification.NotificationDao.NotificationMeta
import hu.ksisu.imazsak.notification.NotificationService
import hu.ksisu.imazsak.prayer.PrayerDao.{
  CreatePrayerData,
  GroupPrayerListData,
  MyPrayerListData,
  PrayerDetailsData,
  PrayerUpdateData,
  PrayerWithPrayUserData
}
import hu.ksisu.imazsak.prayer.PrayerService.{
  CreatePrayerRequest,
  Next10PrayerListData,
  PrayerCloseFeedbackNotificationData,
  PrayerCreatedNotificationData,
  PrayerDetailsResponse,
  PrayerUpdateRequest
}
import hu.ksisu.imazsak.prayer.PrayerServiceImpl._
import hu.ksisu.imazsak.stat.StatService
import hu.ksisu.imazsak.user.UserDao
import hu.ksisu.imazsak.util.DateTimeUtil
import hu.ksisu.imazsak.util.LoggerUtil.{Logger, UserLogContext}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.duration._

class PrayerServiceImpl[F[_]: MonadError[*[_], Throwable]](implicit
    prayerDao: PrayerDao[F],
    groupDao: GroupDao[F],
    groupService: GroupService[F],
    userDao: UserDao[F],
    notificationService: NotificationService[F],
    cache: CacheService[F],
    stat: StatService[F],
    date: DateTimeUtil
) extends PrayerService[F] {
  import cats.syntax.applicativeError._
  import cats.syntax.functor._
  import cats.syntax.traverse._
  private type Tmp[T] = Response[F, T]

  private implicit val logger  = new Logger("PrayerServiceImpl")
  private val myListTtl        = Some(30.minutes)
  private val prayerDetailsTtl = Some(30.minutes)
  private val groupListTtl     = Some(30.minutes)

  override def createPrayer(data: CreatePrayerRequest)(implicit ctx: UserLogContext): Response[F, Unit] = {
    val model = CreatePrayerData(ctx.userId, data.message, data.groupIds)
    for {
      _  <- checkMessage(data.message)
      _  <- groupService.checkGroups(data.groupIds)
      id <- EitherT.right(prayerDao.createPrayer(model))
      _  <- sendNewPrayerNotificationWithoutError(id, data)
      _  <- EitherT.right(cache.remove(CacheService.myPrayerListKey(ctx.userId)))
      _  <- EitherT.right(invalidateGroupsListCache(data.groupIds))
      _  <- stat.prayerCreated(data)
    } yield {}
  }

  override def getPrayerDetails(prayerId: String)(implicit ctx: UserLogContext): Response[F, PrayerDetailsResponse] = {
    val prayerO = cache.findOrSet(CacheService.prayerDetailsKey(prayerId), prayerDetailsTtl) {
      prayerDao.findByIdWithUpdates(prayerId).value
    }
    for {
      prayer <- EitherT.fromOptionF(prayerO, notFound(prayerId))
      _      <- groupService.checkGroups(prayer.groupIds)
    } yield {
      PrayerDetailsResponse(prayer.id, prayer.userId, prayer.message, prayer.createdAt, prayer.updates)
    }
  }

  override def addUpdateToPrayer(data: PrayerUpdateRequest)(implicit ctx: UserLogContext): Response[F, Unit] = {
    for {
      _ <- loadPrayerAndCheckPrayerBelongsToCurrentUser(data.id)
      _ <- EitherT.right(prayerDao.addUpdate(data.id, PrayerUpdateData(data.message, date.getCurrentTimeMillis)))
      _ <- EitherT.right(cache.remove(CacheService.prayerDetailsKey(data.id)))
    } yield ()
  }

  private def sendNewPrayerNotificationWithoutError(id: String, data: CreatePrayerRequest)(implicit
      ctx: UserLogContext
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
    val result = cache.findOrSet(CacheService.myPrayerListKey(ctx.userId), myListTtl) {
      prayerDao.findPrayerByUser(ctx.userId)
    }
    EitherT.right(result)
  }

  override def listGroupPrayers(
      groupId: String
  )(implicit ctx: UserLogContext): Response[F, Seq[GroupPrayerListData]] = {
    lazy val groupList = cache.findOrSet(CacheService.prayerListByGroupKey(groupId), groupListTtl) {
      prayerDao.findByGroup(groupId)
    }
    for {
      _      <- groupService.checkGroups(Seq(groupId))
      result <- EitherT.right(groupList)
    } yield result
  }

  override def pray(groupId: String, prayerId: String)(implicit ctx: UserLogContext): Response[F, Unit] = {
    for {
      prayerO <- listGroupPrayers(groupId).map(_.find(_.id == prayerId))
      prayer  <- EitherT.fromOption(prayerO, illegalAccessToPrayer(groupId, prayerId))
      _       <- EitherT.right(prayerDao.incrementPrayCount(ctx.userId, prayerId))
      _       <- EitherT.right(cache.remove(CacheService.myPrayerListKey(prayer.userId)))
      _       <- stat.prayed(prayerId)
    } yield ()
  }

  override def next10(groupIds: Seq[String])(implicit ctx: UserLogContext): Response[F, Seq[Next10PrayerListData]] = {
    for {
      _      <- groupService.checkGroups(groupIds)
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
      _          <- EitherT.right(cache.remove(CacheService.myPrayerListKey(ctx.userId)))
      _          <- EitherT.right(invalidateGroupsListCache(prayerData.groupIds))
      _          <- EitherT.right(cache.remove(CacheService.prayerDetailsKey(data.id)))
      _          <- stat.prayerClosed(data)
    } yield ()
  }

  // TODO: refactor the whole function
  private def sendNewPrayerNotification(prayerId: String, data: CreatePrayerRequest)(implicit
      ctx: UserLogContext
  ): Response[F, Unit] = {
    val usersWithGroupIds = data.groupIds.toList
      .flatTraverse(id =>
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
          createPrayerCreatedNotificationData(prayerId, data.message, groupIds).flatMap { msg =>
            notificationService.createNotification(NotificationMeta.PRAYER_CREATED, userId, msg)
          }
      })
      .map(_ => {})
  }

  private def createPrayerCreatedNotificationData(prayerId: String, message: String, groupIds: Seq[String])(implicit
      ctx: UserLogContext
  ): Response[F, PrayerCreatedNotificationData] = {
    val userNameFO = userDao
      .findUserData(ctx.userId)
      .map(_.name)
      .getOrElse(None)
    val result = userNameFO.map { userNameO =>
      PrayerCreatedNotificationData(prayerId, userNameO, message, groupIds)
    }
    EitherT.right(result)
  }

  private def sendFeedbackToUsers(prayerData: PrayerWithPrayUserData, feedback: String)(implicit
      ctx: UserLogContext
  ): Response[F, Unit] = {
    if (feedback.trim.isEmpty) {
      EitherT.rightT({})
    } else {
      createPrayerCloseFeedbackNotificationData(prayerData, feedback).flatMap { msg =>
        prayerData.prayUsers.toList
          .traverse[Tmp, Unit](userId =>
            notificationService.createNotification(NotificationMeta.PRAYER_CLOSE_FEEDBACK, userId, msg)
          )
          .map(_ => {})
      }
    }
  }

  private def createPrayerCloseFeedbackNotificationData(prayerData: PrayerWithPrayUserData, feedback: String)(implicit
      ctx: UserLogContext
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

  private def invalidateGroupsListCache(groupIds: Seq[String]): F[List[Unit]] = {
    groupIds.toList.traverse[F, Unit](id => cache.remove(CacheService.prayerListByGroupKey(id)))
  }

  private def checkMessage(message: String): Response[F, Unit] = {
    EitherT.cond(message.trim.length > 5, (), noMessageError)
  }

  private def noMessageError: AppError = {
    IllegalArgumentError("Must have add message to create a new prayer!")
  }

  private def illegalAccessToPrayer(groupId: String, prayerId: String): AppError = {
    AccessDeniedError(s"Prayer $prayerId not in the group: $groupId")
  }

  private def notFound(prayerId: String): AppError = {
    NotFoundError(s"Prayer ${prayerId} not found")
  }

  private def notTheCurrentUsersPrayer(prayerId: String)(implicit ctx: UserLogContext): AppError = {
    AccessDeniedError(s"Prayer $prayerId not belongs to user ${ctx.userId}")
  }
}

object PrayerServiceImpl {
  implicit val myPrayerListDataFormat: RootJsonFormat[MyPrayerListData]       = jsonFormat5(MyPrayerListData)
  implicit val groupPrayerListDataFormat: RootJsonFormat[GroupPrayerListData] = jsonFormat3(GroupPrayerListData)
  implicit val prayerUpdateDataFormat: RootJsonFormat[PrayerUpdateData]       = jsonFormat2(PrayerUpdateData)
  implicit val prayerDetailsDataFormat: RootJsonFormat[PrayerDetailsData]     = jsonFormat6(PrayerDetailsData)
  implicit val prayerWithPrayUserDataFormat: RootJsonFormat[PrayerWithPrayUserData] = jsonFormat4(
    PrayerWithPrayUserData
  )
}
