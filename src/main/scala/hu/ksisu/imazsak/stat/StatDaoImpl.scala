package hu.ksisu.imazsak.stat

import cats.effect.IO
import hu.ksisu.imazsak.core.dao.MongoSelectors._
import hu.ksisu.imazsak.core.dao.{MongoDatabaseService, MongoQueryHelper}
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.document

import scala.concurrent.ExecutionContext

class StatDaoImpl(implicit mongoDatabaseService: MongoDatabaseService[IO], ec: ExecutionContext, cs: ContextShift[IO])
    extends StatDao[IO] {

  protected implicit val collectionF: IO[BSONCollection] = mongoDatabaseService.getCollection("stats")

  override def incrementStat(key: String, dateKey: String, counter: Long): IO[Unit] = {
    val modifier = document("$inc" -> document("total" -> counter, dateKey -> counter))
    MongoQueryHelper.updateOne(byId(key), modifier, upsert = true)
  }
}
