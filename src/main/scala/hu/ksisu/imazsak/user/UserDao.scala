package hu.ksisu.imazsak.user

import cats.data.OptionT
import hu.ksisu.imazsak.user.UserDao.{UserAdminListData, UserData}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros, document}

trait UserDao[F[_]] {
  def findUserData(id: String): OptionT[F, UserData]
  def updateUserData(userData: UserData): F[Unit]
  def allUser(): F[Seq[UserAdminListData]]
}

object UserDao {
  case class UserData(id: String, name: Option[String])
  case class UserAdminListData(id: String, name: Option[String])

  implicit def userDataWriter: BSONDocumentWriter[UserData]                   = Macros.writer[UserData]
  implicit def userDataReader: BSONDocumentReader[UserData]                   = Macros.reader[UserData]
  implicit def userAdminListDataReader: BSONDocumentReader[UserAdminListData] = Macros.reader[UserAdminListData]

  val userDataProjector: Option[BSONDocument]          = Option(document("id" -> 1, "name" -> 1))
  val userAdminListDataProjector: Option[BSONDocument] = Option(document("id" -> 1, "name" -> 1))
}
