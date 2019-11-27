package hu.ksisu.imazsak.feedback

import cats.effect.{ContextShift, IO}
import hu.ksisu.imazsak.core.dao.{MongoDatabaseService, MongoQueryHelper}
import hu.ksisu.imazsak.feedback.FeedbackDao.CreateFeedbackData
import hu.ksisu.imazsak.util.IdGenerator
import reactivemongo.api.bson.collection.BSONCollection

import scala.concurrent.ExecutionContext

class FeedbackDaoImpl(
    implicit mongoDatabaseService: MongoDatabaseService[IO],
    idGenerator: IdGenerator,
    ec: ExecutionContext,
    cs: ContextShift[IO]
) extends FeedbackDao[IO] {
  protected implicit val collectionF: IO[BSONCollection] = mongoDatabaseService.getCollection("feedback")

  override def create(data: CreateFeedbackData): IO[String] = {
    MongoQueryHelper.insert(data)
  }

}
