package hu.ksisu.imazsak.core.dao

import hu.ksisu.imazsak.util.IdGenerator
import reactivemongo.api.bson.{BSON, BSONDocument, BSONDocumentWriter, BSONString, document}

object BsonHelper {

  implicit class ModelWrapper[T](model: T) {

    def toBsonWithNewId(
        implicit writer: BSONDocumentWriter[T],
        idGenerator: IdGenerator
    ): BSONDocument = {
      val id = idGenerator.generate()
      BSON.writeDocument(model).getOrElse(document()) ++ BSONDocument("id" -> BSONString(id))
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
