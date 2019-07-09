package hu.ksisu.imazsak.core.dao

import hu.ksisu.imazsak.Initable
import reactivemongo.api.collections.bson.BSONCollection

trait MongoDatabaseService[F[_]] extends Initable[F] {
  def checkStatus(): F[Boolean]
  def getCollection(name: String): F[BSONCollection]
}

object MongoDatabaseService {
  case class MongoConfig(uri: String)
}
