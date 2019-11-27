package hu.ksisu.imazsak.core.dao

import reactivemongo.api.bson.{BSONDocument, document}

object MongoProjectors {
  val existsProjector: Option[BSONDocument] = Some(document("_id" -> 1))
}
