package hu.ksisu.imazsak.user

import cats.data.OptionT
import hu.ksisu.imazsak.user.UserDao.{UserAdminListData, UserData}
import reactivemongo.bson.{BSONDocument, BSONDocumentHandler, BSONDocumentReader, Macros, document}

trait UserDao[F[_]] {
  def findUserData(id: String): OptionT[F, UserData]
  def updateUserData(userData: UserData): F[Unit]
  def allUser(): F[Seq[UserAdminListData]]
}

object UserDao {
  case class UserData(id: String, name: Option[String])
  case class UserAdminListData(id: String, name: Option[String])

  implicit val userDataHandler: BSONDocumentHandler[UserData]                 = Macros.handler[UserData]
  implicit val userAdminListDataReader: BSONDocumentReader[UserAdminListData] = Macros.reader[UserAdminListData]

  val userDataProjector: Option[BSONDocument]          = Option(document("id" -> 1, "name" -> 1))
  val userAdminListDataProjector: Option[BSONDocument] = Option(document("id" -> 1, "name" -> 1))
}
