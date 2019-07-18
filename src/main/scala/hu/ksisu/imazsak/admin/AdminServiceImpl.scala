package hu.ksisu.imazsak.admin

import cats.Monad
import cats.data.EitherT
import hu.ksisu.imazsak.Errors.Response
import hu.ksisu.imazsak.admin.AdminService.{AddUserToGroupRequest, CreateGroupRequest}
import hu.ksisu.imazsak.core.dao.GroupDao.{CreateGroupData, GroupAdminListData, GroupMember}
import hu.ksisu.imazsak.core.dao.UserDao.UserAdminListData
import hu.ksisu.imazsak.core.dao.{GroupDao, UserDao}
import hu.ksisu.imazsak.util.LoggerUtil.AdminLogContext

class AdminServiceImpl[F[_]: Monad](implicit userDao: UserDao[F], groupDao: GroupDao[F]) extends AdminService[F] {

  override def listAllGroup()(implicit ctx: AdminLogContext): Response[F, Seq[GroupAdminListData]] = {
    EitherT.right(groupDao.allGroup())
  }

  override def listAllUser()(implicit ctx: AdminLogContext): Response[F, Seq[UserAdminListData]] = {
    EitherT.right(userDao.allUser())
  }

  override def addUserToGroup(data: AddUserToGroupRequest)(implicit ctx: AdminLogContext): Response[F, Unit] = {
    for {
      _ <- validateMemberJoin(data)
      _ <- EitherT.right(groupDao.addMemberToGroup(data.groupId, GroupMember(data.userId)))
    } yield ()
  }

  override def createGroup(data: CreateGroupRequest)(implicit ctx: AdminLogContext): Response[F, Unit] = {
    for {
      _ <- validateName(data.name)
      model = CreateGroupData(data.name, Seq(GroupMember(data.adminUserId)))
      _ <- EitherT.right(groupDao.createGroup(model))
    } yield ()
  }

  private def validateMemberJoin(data: AddUserToGroupRequest): Response[F, Unit] = {
    EitherT
      .right(groupDao.isMember(data.groupId, data.userId))
      .ensure(userIsAlreadyMember(data.groupId, data.userId))(!_)
      .map(_ => ())
  }

  private def validateName(name: String): Response[F, Unit] = {
    for {
      _ <- EitherT.cond[F](name.trim.length > 5, (), nameIsTooShort(name))
      _ <- groupDao.findGroupByName(name).map(_ => nameIsUsed(name)).toLeft(())
    } yield ()
  }

  private def nameIsUsed(name: String): Throwable = {
    new IllegalArgumentException(s"Group name: $name is already used!")
  }

  private def nameIsTooShort(name: String): Throwable = {
    new IllegalArgumentException(s"Group name: $name is too short!")
  }

  private def userIsAlreadyMember(groupId: String, userId: String): Throwable = {
    new IllegalArgumentException(s"User $userId is already member in $groupId!")
  }

}
