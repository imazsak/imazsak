package hu.ksisu.imazsak.user

import cats.data.OptionT
import hu.ksisu.imazsak.user.UserDao.{UserAdminListData, UserData, UserPushSubscriptionData}
import reactivemongo.api.bson.{BSONDocument, BSONDocumentHandler, BSONDocumentReader, Macros, document}

trait UserDao[F[_]] {
  def findUserData(id: String): OptionT[F, UserData]
  def updateUserData(userData: UserData): F[Unit]
  def allUser(): F[Seq[UserAdminListData]]
  def findUsersByIds(ids: Seq[String]): F[Seq[UserAdminListData]]
  def isAdmin(id: String): F[Boolean]
  def addPushSubscription(userId: String, deviceId: String, data: UserPushSubscriptionData): F[Unit]
  def removePushSubscriptionByDeviceId(deviceId: String): F[Unit]
  def findPushSubscriptionsByUserId(userId: String): F[Seq[UserPushSubscriptionData]]
}

object UserDao {
  case class UserData(id: String, name: Option[String])
  case class UserAdminListData(id: String, name: Option[String])
  case class UserPushSubscriptionData(
      endpoint: String,
      expirationTime: Option[Long],
      keys: Map[String, String]
  )

  implicit val userDataHandler: BSONDocumentHandler[UserData]                 = Macros.handler[UserData]
  implicit val userAdminListDataReader: BSONDocumentReader[UserAdminListData] = Macros.reader[UserAdminListData]
  implicit val userPushSubscriptionDataReader: BSONDocumentHandler[UserPushSubscriptionData] =
    Macros.handler[UserPushSubscriptionData]

  val userDataProjector: Option[BSONDocument]             = Option(document("id"      -> 1, "name" -> 1))
  val userAdminListDataProjector: Option[BSONDocument]    = Option(document("id"      -> 1, "name" -> 1))
  val isAdminProjector: Option[BSONDocument]              = Option(document("isAdmin" -> 1))
  val findPushSubscriptionProjector: Option[BSONDocument] = Option(document("push"    -> 1))
}
