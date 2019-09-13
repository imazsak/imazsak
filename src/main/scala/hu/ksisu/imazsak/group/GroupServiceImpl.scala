package hu.ksisu.imazsak.group

import cats.Monad
import cats.data.EitherT
import hu.ksisu.imazsak.Errors.{AccessDeniedError, AppError, IllegalArgumentError, Response}
import hu.ksisu.imazsak.group.GroupDao.{GroupListData, GroupMember}
import hu.ksisu.imazsak.group.GroupServiceImpl.GroupTokenData
import hu.ksisu.imazsak.token.TokenService
import hu.ksisu.imazsak.token.TokenService.CreateTokenData
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext
import spray.json.RootJsonFormat

class GroupServiceImpl[F[_]: Monad](implicit val groupDao: GroupDao[F], tokenService: TokenService[F])
    extends GroupService[F] {
  private val tokenType = "GROUP_JOIN"

  override def listGroups()(implicit ctx: UserLogContext): Response[F, Seq[GroupListData]] = {
    EitherT.right(groupDao.findGroupsByUser(ctx.userId))
  }

  override def createJoinToken(groupId: String)(implicit ctx: UserLogContext): Response[F, String] = {
    for {
      _     <- EitherT.right(groupDao.isMember(groupId, ctx.userId)).ensure(illegalGroupError(groupId))(identity)
      token <- EitherT.right(tokenService.createToken(createTokenData(groupId)))
    } yield token
  }

  override def joinToGroup(groupId: String, token: String)(implicit ctx: UserLogContext): Response[F, Unit] = {
    for {
      _      <- EitherT.right(groupDao.isMember(groupId, ctx.userId)).ensure(alreadyMember(groupId))(!_)
      tokenO <- tokenService.validateAndGetTokenData[GroupTokenData](tokenType, token)
      _      <- EitherT.fromOption(tokenO, invalidToken).ensure(invalidToken)(_.groupId == groupId)
      _      <- EitherT.right(groupDao.addMemberToGroup(groupId, GroupMember(ctx.userId)))
    } yield ()
  }

  private def createTokenData(groupId: String) = {
    CreateTokenData(tokenType, Some(GroupTokenData(groupId)))
  }

  private def illegalGroupError(groupId: String)(implicit ctx: UserLogContext): AppError = {
    AccessDeniedError(s"User ${ctx.userId} not member in: $groupId}")
  }

  private def alreadyMember(groupId: String)(implicit ctx: UserLogContext): AppError = {
    IllegalArgumentError(s"User ${ctx.userId} is already member in: $groupId}")
  }

  private val invalidToken: AppError = IllegalArgumentError("Invalid Token")

}

object GroupServiceImpl {
  import spray.json.DefaultJsonProtocol._
  case class GroupTokenData(groupId: String)
  implicit val groupTokenDataFormatter: RootJsonFormat[GroupTokenData] = jsonFormat1(GroupTokenData)
}
