package hu.ksisu.imazsak.group

import cats.Monad
import cats.data.EitherT
import hu.ksisu.imazsak.Errors.{AccessDeniedError, AppError, IllegalArgumentError, Response}
import hu.ksisu.imazsak.core.CacheService
import hu.ksisu.imazsak.group.GroupDao.{GroupListData, GroupMember}
import hu.ksisu.imazsak.group.GroupServiceImpl._
import hu.ksisu.imazsak.token.TokenService
import hu.ksisu.imazsak.token.TokenService.CreateTokenData
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.duration._

class GroupServiceImpl[F[_]: Monad](
    implicit val groupDao: GroupDao[F],
    tokenService: TokenService[F],
    cache: CacheService[F]
) extends GroupService[F] {
  private val tokenType    = "GROUP_JOIN"
  private val groupListTtl = Some(30.minutes)

  override def listGroups()(implicit ctx: UserLogContext): Response[F, Seq[GroupListData]] = {
    val result = cache.findOrSet(CacheService.groupListByUserKey(ctx.userId), groupListTtl) {
      groupDao.findGroupsByUser(ctx.userId)
    }
    EitherT.right(result)
  }

  override def createJoinToken(groupId: String)(implicit ctx: UserLogContext): Response[F, String] = {
    for {
      _     <- EitherT.right(groupDao.isMember(groupId, ctx.userId)).ensure(illegalGroupError(Set(groupId)))(identity)
      token <- EitherT.right(tokenService.createToken(createTokenData(groupId)))
    } yield token
  }

  override def joinToGroup(token: String)(implicit ctx: UserLogContext): Response[F, Unit] = {
    for {
      tokenO <- tokenService.validateAndGetTokenData[GroupTokenData](tokenType, token)
      data   <- EitherT.fromOption(tokenO, invalidToken)
      _      <- EitherT.right(groupDao.isMember(data.groupId, ctx.userId)).ensure(alreadyMember(data.groupId))(!_)
      _      <- EitherT.right(groupDao.addMemberToGroup(data.groupId, GroupMember(ctx.userId)))
      _      <- EitherT.right(cache.remove(CacheService.groupListByUserKey(ctx.userId)))
    } yield ()
  }

  override def checkGroups(groupIds: Seq[String])(implicit ctx: UserLogContext): Response[F, Unit] = {
    for {
      _      <- EitherT.cond(groupIds.nonEmpty, (), noGroupError)
      groups <- listGroups()
      illegalGroups = groupIds.toSet -- groups.map(_.id).toSet
      _ <- EitherT.cond(illegalGroups.isEmpty, (), illegalGroupError(illegalGroups))
    } yield ()
  }

  private def createTokenData(groupId: String): CreateTokenData[GroupTokenData] = {
    CreateTokenData(tokenType, Some(GroupTokenData(groupId)))
  }

  private def illegalGroupError(groups: Set[String])(implicit ctx: UserLogContext): AppError = {
    AccessDeniedError(s"User ${ctx.userId} not member in: ${groups.mkString("[,", ",", "]")}")
  }

  private def noGroupError: AppError = {
    IllegalArgumentError("Must have at least one group to create a new prayer!")
  }

  private def alreadyMember(groupId: String)(implicit ctx: UserLogContext): AppError = {
    IllegalArgumentError(s"User ${ctx.userId} is already member in: $groupId}")
  }

  private val invalidToken: AppError = IllegalArgumentError("Invalid Token")
}

object GroupServiceImpl {
  case class GroupTokenData(groupId: String)
  implicit val groupTokenDataFormatter: RootJsonFormat[GroupTokenData] = jsonFormat1(GroupTokenData)
  implicit val groupListDataFormat: RootJsonFormat[GroupListData]      = jsonFormat2(GroupListData)
}
