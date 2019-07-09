package hu.ksisu.imazsak

import hu.ksisu.imazsak.core.AmqpService
import hu.ksisu.imazsak.core.TracerService.TracerServiceConfig
import hu.ksisu.imazsak.core.config.ServerConfig
import hu.ksisu.imazsak.core.dao.MongoDatabaseService.MongoConfig
import hu.ksisu.imazsak.core.impl.JwtServiceImpl
import org.slf4j.LoggerFactory

import scala.util.{Success, Try}

class InitiableSpec extends TestBase {

  trait TestScope {
    var called = false
    var loaded = false
    lazy val service = new Initable[Try] {
      loaded = true
      override def init: Try[Unit] = {
        called = true
        Success(())
      }
    }
    implicit val logger = LoggerFactory.getLogger("test")
    implicit val config = new ServerConfig[Try] {
      override def getEnabledModules: Seq[String]              = Seq("modulename")
      override def init: Try[Unit]                             = ???
      override def getMongoConfig: MongoConfig                 = ???
      override def getTracerServiceConfig: TracerServiceConfig = ???
      override def getAmqpConfig: AmqpService.AmqpConfig       = ???
      override def getJwtConfig: JwtServiceImpl.JwtConfig      = ???
    }
  }
  import cats.instances.try_._

  "Initiable" should {
    "#initializeIfEnabled" should {
      "call init if enabled" in new TestScope {
        Initable.initializeIfEnabled(service, "modulename") shouldEqual Success(())
        called shouldBe true
      }
      "do not call init if disabled" in new TestScope {
        Initable.initializeIfEnabled(service, "modulename2") shouldEqual Success(())
        called shouldBe false
      }
      "do not instantiating the service" in new TestScope {
        Initable.initializeIfEnabled(service, "modulename2") shouldEqual Success(())
        loaded shouldBe false
      }
    }
  }

}
