package hu.ksisu.imazsak.core.dao

import cats.effect.IO
import hu.ksisu.imazsak.AwaitUtil
import hu.ksisu.imazsak.core.dao.BsonHelper._
import hu.ksisu.imazsak.stat.StatDao
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import reactivemongo.api.Cursor
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{BSONDocument, BSONLong, _}

import scala.concurrent.ExecutionContext.Implicits.global

trait StatDaoSpec {
  self: AnyWordSpecLike with Matchers with AwaitUtil =>

  protected val statDao: StatDao[IO]
  protected val statCollection: BSONCollection

  def statDaoTests(): Unit = {
    "StatDao" when {
      def getAllStat(): Map[String, BSONDocument] = {
        val result = await(
          statCollection
            .find(BSONDocument(), None)
            .cursor[BSONDocument]()
            .collect[Seq](-1, Cursor.FailOnError[Seq[BSONDocument]]())
        )
        result.map(doc => doc.getId -> doc).toMap
      }
      "#incrementStat" in {
        val key1     = "stat-key"
        val dateKey1 = "2010-05"
        val dateKey2 = "2010-06"

        getAllStat().isEmpty shouldEqual true

        statDao.incrementStat(key1, dateKey1, counter = 1).unsafeRunSync()

        val resultMap = getAllStat()
        resultMap(key1).get("total") shouldEqual Some(BSONLong(1))
        resultMap(key1).get(dateKey1) shouldEqual Some(BSONLong(1))
        resultMap(key1).get(dateKey2) shouldEqual None

        statDao.incrementStat(key1, dateKey1, counter = 2).unsafeRunSync()
        statDao.incrementStat(key1, dateKey2, counter = 2).unsafeRunSync()

        val resultMap2 = getAllStat()
        resultMap2(key1).get("total") shouldEqual Some(BSONLong(5))
        resultMap2(key1).get(dateKey1) shouldEqual Some(BSONLong(3))
        resultMap2(key1).get(dateKey2) shouldEqual Some(BSONLong(2))
      }
    }
  }
}
