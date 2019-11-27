package hu.ksisu.imazsak.core.dao
import reactivemongo.api.bson.{BSONDocument, document}

object MongoSelectors {
  val all: BSONDocument                                  = document()
  def byId(id: String): BSONDocument                     = document("id" -> id)
  def byUserId(id: String): BSONDocument                 = document("userId" -> id)
  def byIds(ids: Seq[String]): BSONDocument              = document("id" -> document("$in" -> ids))
  def byOptionalUserId(id: Option[String]): BSONDocument = id.map(byUserId).getOrElse(document())
  def byName(name: String): BSONDocument                 = document("name" -> name)
  def groupIdsContains(id: String): BSONDocument         = document("groupIds" -> id)
  def memberIdsContains(userId: String): BSONDocument    = document("members.id" -> userId)
}
