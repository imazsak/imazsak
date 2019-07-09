package hu.ksisu.imazsak

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.slf4j.LoggerFactory

trait RouteTestBase extends TestBase with ScalatestRouteTest {
  trait BaseTestScope {
    lazy val services     = new ItTestServices {}
    implicit val logger   = LoggerFactory.getLogger("TEST")
    lazy val route: Route = Api.createApi(services)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(system.terminate())
  }
}
