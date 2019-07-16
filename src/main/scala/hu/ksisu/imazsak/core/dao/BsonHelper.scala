package hu.ksisu.imazsak.core.dao

import hu.ksisu.imazsak.util.IdGenerator
import reactivemongo.bson.{BSON, BSONDocument, BSONString, BSONWriter}

object BsonHelper {

  implicit class ModelWrapper[T](model: T) {

    def toBsonWithNewId[B <: BSONDocument](
        implicit writer: BSONWriter[T, B],
        idGenerator: IdGenerator
    ): BSONDocument = {
      val id = idGenerator.generate()
      BSON.write(model) ++ BSONDocument("id" -> BSONString(id))
    }
  }

  implicit class BsonWrapper(bson: BSONDocument) {
    def getId: String = {
      bson
        .get("id")
        .collect {
          case BSONString(id) => id
        }
        .getOrElse(throw new IllegalArgumentException(s"id not found in $bson"))
    }
  }
}
