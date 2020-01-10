package hu.ksisu.imazsak.core.dao

import cats.effect.IO
import hu.ksisu.imazsak.AwaitUtil
import hu.ksisu.imazsak.token.TokenDao
import hu.ksisu.imazsak.token.TokenDao.TokenData
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import reactivemongo.api.bson.collection.BSONCollection

trait TokenDaoSpec {

  self: AnyWordSpecLike with Matchers with AwaitUtil =>

  protected val tokenDao: TokenDao[IO]
  protected val tokenCollection: BSONCollection

  def tokenDaoTests(): Unit = {
    "TokenDao" when {
      "#create and #findByTypeAndToken" in {
        val data1 = TokenData("type1", "token1", Some("data1"), Some(1111), false)
        val data2 = TokenData("type2", "token2", None, None, true)
        val data3 = TokenData("type1", "token2", Some("data3"), None, true)

        tokenDao.create(data1).unsafeRunSync()
        tokenDao.create(data2).unsafeRunSync()
        tokenDao.create(data3).unsafeRunSync()

        tokenDao.findByTypeAndToken("type1", "token1").value.unsafeRunSync() shouldEqual Some(data1)
        tokenDao.findByTypeAndToken("type2", "token2").value.unsafeRunSync() shouldEqual Some(data2)
        tokenDao.findByTypeAndToken("type1", "token2").value.unsafeRunSync() shouldEqual Some(data3)
        tokenDao.findByTypeAndToken("type2", "token1").value.unsafeRunSync() shouldEqual None
      }
      "#deleteByTypeAndToken" in {
        val data1 = TokenData("type1", "token1", Some("data1"), Some(1111), false)

        tokenDao.create(data1).unsafeRunSync()

        tokenDao.findByTypeAndToken("type1", "token1").value.unsafeRunSync() shouldEqual Some(data1)
        tokenDao.deleteByTypeAndToken("type1", "token1").unsafeRunSync()
        tokenDao.findByTypeAndToken("type1", "token1").value.unsafeRunSync() shouldEqual None
      }
      "#deleteByExpiredAt" in {
        val data1 = TokenData("type1", "token1", Some("data1"), Some(1111), false)
        val data2 = TokenData("type2", "token2", None, None, true)
        val data3 = TokenData("type1", "token2", Some("data3"), Some(2222), true)

        tokenDao.create(data1).unsafeRunSync()
        tokenDao.create(data2).unsafeRunSync()
        tokenDao.create(data3).unsafeRunSync()

        tokenDao.deleteByExpiredAt(0).unsafeRunSync() shouldEqual 0
        tokenDao.deleteByExpiredAt(1500).unsafeRunSync() shouldEqual 1

        tokenDao.findByTypeAndToken("type1", "token1").value.unsafeRunSync() shouldEqual None
        tokenDao.findByTypeAndToken("type2", "token2").value.unsafeRunSync() shouldEqual Some(data2)
        tokenDao.findByTypeAndToken("type1", "token2").value.unsafeRunSync() shouldEqual Some(data3)

        tokenDao.deleteByExpiredAt(3000).unsafeRunSync() shouldEqual 1
        tokenDao.deleteByExpiredAt(3000).unsafeRunSync() shouldEqual 0

        tokenDao.findByTypeAndToken("type2", "token2").value.unsafeRunSync() shouldEqual Some(data2)
        tokenDao.findByTypeAndToken("type1", "token2").value.unsafeRunSync() shouldEqual None
      }
    }
  }
}
