package hu.ksisu.imazsak.core.impl

import akka.actor.ActorSystem
import cats.effect.{ContextShift, IO}
import hu.ksisu.imazsak.core.RedisService
import hu.ksisu.imazsak.core.RedisService.RedisConfig
import redis.{RedisClientPool, RedisServer}

class RedisServiceImpl(implicit val config: RedisConfig, as: ActorSystem, cs: ContextShift[IO])
    extends RedisService[IO] {

  private lazy val client = RedisClientPool(
    Seq(RedisServer(config.host, config.port, config.password, config.database))
  )

  override def init: IO[Unit] = IO {
    checkStatus().map(_ => ())
  }

  override def checkStatus(): IO[Boolean] = {
    IO.fromFuture(IO(client.ping())).redeem(_ => false, _ => true)
  }
}
