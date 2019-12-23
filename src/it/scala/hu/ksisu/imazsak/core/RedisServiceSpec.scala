package hu.ksisu.imazsak.core

import akka.actor.ActorSystem
import akka.testkit.TestKit
import cats.effect.IO
import hu.ksisu.imazsak.AwaitUtil
import hu.ksisu.imazsak.core.config.ServerConfigImpl
import hu.ksisu.imazsak.core.impl.RedisServiceImpl
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.ExecutionContext
import scala.util.Try

class RedisServiceSpec
    extends TestKit(ActorSystem("RedisServiceSpec"))
    with WordSpecLike
    with Matchers
    with AwaitUtil
    with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import system.dispatcher
  private implicit val contextShift = IO.contextShift(implicitly[ExecutionContext])

  import cats.instances.try_._
  private val conf = new ServerConfigImpl[Try]
  import conf._

  private val redisService = new RedisServiceImpl()

  "RedisService" when {
    "#checkStatus" in {
      redisService.checkStatus().unsafeRunSync() shouldEqual true
    }
  }
}
