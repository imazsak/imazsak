package hu.ksisu.imazsak.core.dao
import reactivemongo.bson.{BSONDocument, document}

object MongoSelectors {
  def byId(id: String): BSONDocument = document("id" -> id)
}
