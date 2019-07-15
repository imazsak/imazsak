package hu.ksisu.imazsak.core.dao

import cats.data.OptionT
import hu.ksisu.imazsak.core.dao.UserDao.UserData
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros, document}

trait UserDao[F[_]] {
  def findUserData(id: String): OptionT[F, UserData]
  def updateUserData(userData: UserData): F[Unit]
}

object UserDao {
  case class UserData(id: String, name: String)

  implicit def userDataWriter: BSONDocumentWriter[UserData] = Macros.writer[UserData]
  implicit def userDataReader: BSONDocumentReader[UserData] = Macros.reader[UserData]

  val userDataProjector: Option[BSONDocument] = Option(document("id" -> 1, "name" -> 1))
}
