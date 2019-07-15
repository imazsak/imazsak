package hu.ksisu.imazsak.core.dao

import hu.ksisu.imazsak.core.dao.GroupDao.GroupListData
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, Macros, document}

trait GroupDao[F[_]] {
  def findGroupsByUser(userId: String): F[Seq[GroupListData]]
}

object GroupDao {
  case class GroupListData(id: String, name: String)
  implicit def groupListDataReader: BSONDocumentReader[GroupListData] = Macros.reader[GroupListData]
  val groupListDataProjector: Option[BSONDocument]                    = Option(document("id" -> 1, "name" -> 1))
}
