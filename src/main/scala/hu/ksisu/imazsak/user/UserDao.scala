package hu.ksisu.imazsak.user

import cats.data.OptionT
import hu.ksisu.imazsak.user.UserDao.{UserAdminListData, UserData, UserPushSubscribeData}
import reactivemongo.api.bson.{BSONDocument, BSONDocumentHandler, BSONDocumentReader, Macros, document}

trait UserDao[F[_]] {
  def findUserData(id: String): OptionT[F, UserData]
  def updateUserData(userData: UserData): F[Unit]
  def allUser(): F[Seq[UserAdminListData]]
  def findUsersByIds(ids: Seq[String]): F[Seq[UserAdminListData]]
  def isAdmin(id: String): F[Boolean]
  def savePushSubscribe(id: String, data: UserPushSubscribeData): F[Unit]
  def removePushSubscribe(id: String): F[Unit]
  def findPushSubscribe(id: String): OptionT[F, UserPushSubscribeData]
}

object UserDao {
  case class UserData(id: String, name: Option[String])
  case class UserAdminListData(id: String, name: Option[String])
  case class UserPushSubscribeData(
      endpoint: String,
      expirationTime: Option[Long],
      keys: Map[String, String]
  )

  implicit val userDataHandler: BSONDocumentHandler[UserData]                 = Macros.handler[UserData]
  implicit val userAdminListDataReader: BSONDocumentReader[UserAdminListData] = Macros.reader[UserAdminListData]
  implicit val userPushSubscribeDataReader: BSONDocumentHandler[UserPushSubscribeData] =
    Macros.handler[UserPushSubscribeData]

  val userDataProjector: Option[BSONDocument]          = Option(document("id"      -> 1, "name" -> 1))
  val userAdminListDataProjector: Option[BSONDocument] = Option(document("id"      -> 1, "name" -> 1))
  val isAdminProjector: Option[BSONDocument]           = Option(document("isAdmin" -> 1))
  val findPushSubscribeProjector: Option[BSONDocument] = Option(document("push"    -> 1))
}
