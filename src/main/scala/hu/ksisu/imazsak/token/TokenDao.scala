package hu.ksisu.imazsak.token

import cats.data.OptionT
import hu.ksisu.imazsak.token.TokenDao.TokenData
import reactivemongo.api.bson.{BSONDocumentHandler, Macros}

trait TokenDao[F[_]] {
  def create(data: TokenData): F[Unit]
  def findByTypeAndToken(tokenType: String, token: String): OptionT[F, TokenData]
  def deleteByTypeAndToken(tokenType: String, token: String): F[Int]
  def deleteByExpiredAt(before: Long): F[Int]
}

object TokenDao {
  case class TokenData(
      tokenType: String,
      token: String,
      data: Option[String],
      expiredAt: Option[Long],
      reusable: Boolean
  )
  implicit val tokenDataWriter: BSONDocumentHandler[TokenData] = Macros.handler[TokenData]
}
