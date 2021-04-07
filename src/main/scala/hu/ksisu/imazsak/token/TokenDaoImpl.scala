package hu.ksisu.imazsak.token

import cats.data.OptionT
import cats.effect.{ContextShift, IO}
import hu.ksisu.imazsak.core.dao.{MongoDatabaseService, MongoQueryHelper}
import hu.ksisu.imazsak.token.TokenDao.TokenData
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{BSONDocument, document}

import scala.concurrent.ExecutionContext

class TokenDaoImpl(implicit
    mongoDatabaseService: MongoDatabaseService[IO],
    ec: ExecutionContext,
    cs: ContextShift[IO]
) extends TokenDao[IO] {
  protected implicit val collectionF: IO[BSONCollection] = mongoDatabaseService.getCollection("tokens")

  override def create(data: TokenData): IO[Unit] = {
    for {
      collection <- collectionF
      _          <- IO.fromFuture(IO(collection.insert(false).one(data)))
    } yield ()
  }

  override def findByTypeAndToken(tokenType: String, token: String): OptionT[IO, TokenData] = {
    MongoQueryHelper.findOne[TokenData](byTypeAndToken(tokenType, token), None)
  }

  override def deleteByTypeAndToken(tokenType: String, token: String): IO[Int] = {
    MongoQueryHelper.deleteOne(byTypeAndToken(tokenType, token))
  }

  override def deleteByExpiredAt(before: Long): IO[Int] = {
    val selector = document("expiredAt" -> document("$lt" -> before))
    MongoQueryHelper.deleteMultiple(selector)
  }

  private def byTypeAndToken(tokenType: String, token: String): BSONDocument = {
    document("tokenType" -> tokenType, "token" -> token)
  }

}
