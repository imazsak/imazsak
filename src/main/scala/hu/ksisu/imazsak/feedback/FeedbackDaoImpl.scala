package hu.ksisu.imazsak.feedback

import hu.ksisu.imazsak.core.dao.{MongoDatabaseService, MongoQueryHelper}
import hu.ksisu.imazsak.feedback.FeedbackDao.CreateFeedbackData
import hu.ksisu.imazsak.util.IdGenerator
import reactivemongo.api.collections.bson.BSONCollection

import scala.concurrent.{ExecutionContext, Future}

class FeedbackDaoImpl(
    implicit mongoDatabaseService: MongoDatabaseService[Future],
    idGenerator: IdGenerator,
    ec: ExecutionContext
) extends FeedbackDao[Future] {
  protected implicit val collectionF: Future[BSONCollection] = mongoDatabaseService.getCollection("feedback")

  override def create(data: CreateFeedbackData): Future[String] = {
    MongoQueryHelper.insert(data)
  }

}
