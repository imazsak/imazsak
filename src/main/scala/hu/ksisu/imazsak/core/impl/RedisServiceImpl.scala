package hu.ksisu.imazsak.core.impl

import akka.actor.ActorSystem
import akka.util.ByteString
import cats.data.OptionT
import cats.effect.IO
import hu.ksisu.imazsak.core.CacheService
import hu.ksisu.imazsak.core.impl.RedisServiceImpl._
import redis.{ByteStringFormatter, RedisClientPool, RedisServer}
import spray.json.JsonFormat

import scala.concurrent.duration.FiniteDuration

class RedisServiceImpl(implicit val config: RedisConfig, as: ActorSystem, cs: ContextShift[IO])
    extends CacheService[IO] {

  private lazy val client = RedisClientPool(
    Seq(RedisServer(config.host, config.port, config.password, config.database))
  )

  override def init: IO[Unit] = IO {
    checkStatus().map(_ => ())
  }

  override def checkStatus(): IO[Boolean] = {
    IO.fromFuture(IO(client.ping())).redeem(_ => false, _ => true)
  }

  override def findOrSet[T](key: String, ttl: Option[FiniteDuration])(
      valueF: => IO[T]
  )(implicit format: JsonFormat[T]): IO[T] = {
    val fromCache: IO[Option[T]] = IO.fromFuture(IO(client.get[T](key)(format)))

    val store: IO[T] = for {
      value <- valueF
      _     <- IO.fromFuture(IO(client.set(key, value, ttl.map(_.toSeconds))(format)))
    } yield value

    OptionT(fromCache).getOrElseF(store)
  }

  override def remove(key: String): IO[Unit] = {
    IO.fromFuture(IO(client.del(key))).map(_ => ())
  }
}

object RedisServiceImpl {
  case class RedisConfig(host: String, port: Int, password: Option[String], database: Option[Int])
  implicit def jsonFormatToByteStringFormatter[T](jsonFormat: JsonFormat[T]): ByteStringFormatter[T] = {
    import spray.json._
    new ByteStringFormatter[T] {
      def serialize(data: T): ByteString = ByteString(data.toJson(jsonFormat).compactPrint)
      def deserialize(bs: ByteString): T = bs.utf8String.parseJson.convertTo(jsonFormat)
    }
  }
}
