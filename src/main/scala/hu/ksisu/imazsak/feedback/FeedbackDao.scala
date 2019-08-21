package hu.ksisu.imazsak.feedback

import hu.ksisu.imazsak.feedback.FeedbackDao.CreateFeedbackData
import reactivemongo.bson.{BSONDocumentWriter, Macros}

trait FeedbackDao[F[_]] {
  def create(data: CreateFeedbackData): F[String]
}

object FeedbackDao {
  case class CreateFeedbackData(userId: String, message: String, createdAt: Long)
  implicit val createFeedbackDataWriter: BSONDocumentWriter[CreateFeedbackData] = Macros.writer[CreateFeedbackData]
}
