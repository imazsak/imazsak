package hu.ksisu.imazsak.token

import cats.Monad
import cats.data.EitherT
import hu.ksisu.imazsak.Errors.{AppError, NotFoundError, Response}
import hu.ksisu.imazsak.token.TokenDao.TokenData
import hu.ksisu.imazsak.token.TokenService.CreateTokenData
import hu.ksisu.imazsak.util.{DateTimeUtil, IdGenerator}

class TokenServiceImpl[F[_]: Monad](
    implicit idGenerator: IdGenerator,
    dateTimeUtil: DateTimeUtil,
    tokenDao: TokenDao[F]
) extends TokenService[F] {
  import cats.syntax.functor._
  import spray.json._

  override def init: F[Unit] = {
    cleanUpExpiredTokens()
  }

  override def createToken[T](data: CreateTokenData[T])(implicit w: JsonWriter[T]): F[String] = {
    val token     = idGenerator.generate()
    val extraData = data.data.map(_.toJson.compactPrint)
    val expiredAt = data.expiredAfter.map { after =>
      dateTimeUtil.getCurrentTimeMillis + after.toMillis
    }
    val tokenData = TokenData(
      data.tokenType,
      token,
      extraData,
      expiredAt,
      data.reusable
    )
    tokenDao.create(tokenData).map(_ => token)
  }

  override def validateAndGetTokenData[T](tokenType: String, token: String)(
      implicit r: JsonReader[T]
  ): Response[F, Option[T]] = {
    val tokenFromDb: EitherT[F, AppError, TokenData] = tokenDao
      .findByTypeAndToken(tokenType, token)
      .toRight[AppError](NotFoundError("Token not found"))
      .ensure(expiredError)(x => x.expiredAt.isEmpty || x.expiredAt.forall(_ > dateTimeUtil.getCurrentTimeMillis))

    tokenFromDb.toOption.filterNot(_.reusable).map { _ =>
      tokenDao.deleteByTypeAndToken(tokenType, token)
    }

    tokenFromDb.map(_.data.map(_.parseJson.convertTo[T]))
  }

  override def revokeToken(tokenType: String, token: String): Response[F, Unit] = {
    EitherT(tokenDao.deleteByTypeAndToken(tokenType, token).map {
      case 1 => Right(())
      case _ => Left(NotFoundError("Token not found"))
    })
  }

  private def expiredError: AppError = {
    cleanUpExpiredTokens()
    NotFoundError("Token not found")
  }

  private def cleanUpExpiredTokens(): F[Unit] = {
    tokenDao.deleteByExpiredAt(dateTimeUtil.getCurrentTimeMillis).map(_ => {})
  }

}
