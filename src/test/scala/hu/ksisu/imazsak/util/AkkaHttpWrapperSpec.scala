package hu.ksisu.imazsak.util

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import cats.effect.IO
import hu.ksisu.imazsak.TestBase

class AkkaHttpWrapperSpec extends TestBase {

  "AkkaHttpWrapper" should {

    "should unmarshall" in {
      import spray.json.DefaultJsonProtocol._
      import spray.json._
      case class TestClass(a: String, b: Int, c: Boolean)
      implicit val TestClassFormatter = jsonFormat3(TestClass)

      val test = TestClass("asd", 5, false)

      withActorSystem { implicit as =>
        implicit val cs: ContextShift[IO] = IO.contextShift(as.dispatcher)

        val http = new AkkaHttpWrapper()
        val entity = http
          .unmarshalEntityTo[TestClass](
            HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, test.toJson.toString))
          )
          .unsafeRunSync()
        entity shouldBe test
      }
    }

  }

}
