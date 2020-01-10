package hu.ksisu.imazsak.core.dao

import cats.effect.IO
import hu.ksisu.imazsak.AwaitUtil
import hu.ksisu.imazsak.core.dao.BsonHelper._
import hu.ksisu.imazsak.feedback.FeedbackDao
import hu.ksisu.imazsak.feedback.FeedbackDao.CreateFeedbackData
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import reactivemongo.api.Cursor
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{BSONDocument, BSONLong, BSONString, _}

import scala.concurrent.ExecutionContext.Implicits.global

trait FeedbackDaoSpec {
  self: AnyWordSpecLike with Matchers with AwaitUtil =>

  protected val feedbackDao: FeedbackDao[IO]
  protected val feedbackCollection: BSONCollection

  def feedbackDaoTests(): Unit = {
    "FeedbackDao" when {
      "#create" in {
        val data1 = CreateFeedbackData("user_1", "message1", 1)
        val data2 = CreateFeedbackData("user_2", "message2", 2)
        feedbackDao.create(data1).unsafeRunSync() shouldEqual "1"
        feedbackDao.create(data2).unsafeRunSync() shouldEqual "2"

        val result = await(
          feedbackCollection
            .find(BSONDocument(), None)
            .cursor[BSONDocument]()
            .collect[Seq](-1, Cursor.FailOnError[Seq[BSONDocument]]())
        )

        val resultMap = result.map(doc => doc.getId -> doc).toMap
        resultMap("1").get("userId") shouldEqual Some(BSONString("user_1"))
        resultMap("1").get("message") shouldEqual Some(BSONString("message1"))
        resultMap("1").get("createdAt") shouldEqual Some(BSONLong(1))
        resultMap("2").get("userId") shouldEqual Some(BSONString("user_2"))
        resultMap("2").get("message") shouldEqual Some(BSONString("message2"))
        resultMap("2").get("createdAt") shouldEqual Some(BSONLong(2))
      }
    }
  }
}
