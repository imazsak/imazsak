package hu.ksisu.imazsak.core

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.testkit.TestKit
import cats.effect.IO
import hu.ksisu.imazsak.AwaitUtil
import hu.ksisu.imazsak.core.config.ServerConfigImpl
import hu.ksisu.imazsak.core.impl.RedisServiceImpl
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

class RedisServiceSpec
    extends TestKit(ActorSystem("RedisServiceSpec"))
    with AnyWordSpecLike
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

    "#findOrSet and #remove" in {
      val key     = UUID.randomUUID().toString
      val counter = new AtomicInteger(0)
      val valueF  = IO { counter.getAndIncrement() }

      import spray.json.DefaultJsonProtocol._

      redisService.findOrSet(key)(valueF).unsafeRunSync() shouldEqual 0
      redisService.findOrSet(key)(valueF).unsafeRunSync() shouldEqual 0
      counter.get() shouldEqual 1

      redisService.remove(key).unsafeRunSync()
      redisService.findOrSet(key)(valueF).unsafeRunSync() shouldEqual 1
      redisService.findOrSet(key)(valueF).unsafeRunSync() shouldEqual 1
      counter.get() shouldEqual 2
    }

    "#findOrSet with ttl" in {
      val key     = UUID.randomUUID().toString
      val counter = new AtomicInteger(0)
      val valueF  = IO { counter.getAndIncrement() }

      import spray.json.DefaultJsonProtocol._

      redisService.findOrSet(key, Some(1.second))(valueF).unsafeRunSync() shouldEqual 0
      counter.get() shouldEqual 1

      Thread.sleep(1100)

      redisService.findOrSet(key, Some(1.second))(valueF).unsafeRunSync() shouldEqual 1
      counter.get() shouldEqual 2

    }
  }
}
