package hu.ksisu.imazsak.core.config
import hu.ksisu.imazsak.TestBase
import hu.ksisu.imazsak.core.dao.MongoDatabaseService.MongoConfig
import hu.ksisu.imazsak.core.impl.JwtServiceImpl.JwtConfig

import scala.util.Try

class ServerConfigSpec extends TestBase {

  "#isModuleEnabled" should {
    val service        = new ServerConfigImpl[Try]()
    val enabledModules = service.getEnabledModules
    "convert to lowecase" in {
      enabledModules should contain("testmodule1")
    }
    "trim config" in {
      enabledModules should contain("testmodule2")
    }
    "drop empty elements" in {
      enabledModules should not contain ("")
    }
  }

  "#getMongoConfig" in {
    val service = new ServerConfigImpl[Try]()
    service.getMongoConfig shouldEqual MongoConfig("URI")
  }

  "#getJwtConfig" in {
    val service = new ServerConfigImpl[Try]()
    val config  = service.getJwtConfig
    config shouldBe JwtConfig(
      "HS256",
      "secret"
    )
  }

}
