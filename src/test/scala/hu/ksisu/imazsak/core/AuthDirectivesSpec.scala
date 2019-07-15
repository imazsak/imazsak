package hu.ksisu.imazsak.core

import akka.http.scaladsl.model.headers.OAuth2BearerToken
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Route}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import hu.ksisu.imazsak.{Api, TestBase}
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import spray.json.{JsObject, JsString}

import scala.concurrent.Future

class AuthDirectivesSpec extends TestBase with ScalatestRouteTest with BeforeAndAfterEach {

  val jwtServiceMock = mock[JwtService[Future]]

  override protected def beforeEach(): Unit = {
    reset(jwtServiceMock)
  }

  trait TestScope {
    val authDirectives = new AuthDirectives with Api {
      override val jwtService: JwtService[Future] = jwtServiceMock
      override def route(): Route                 = ???
    }

    def route(): Route = authDirectives.userAuth { userId =>
      complete(userId)
    }
  }

  "AuthDirectives" should {
    "#userAuth" should {
      "success if jwt is valid and contains id" in new TestScope {
        when(jwtServiceMock.validateAndDecode(any[String]))
          .thenReturn(Future.successful(Some(JsObject("id" -> JsString("USER_ID")))))
        Get() ~> addCredentials(OAuth2BearerToken("TOKEN")) ~> route ~> check {
          responseAs[String] shouldEqual "USER_ID"
        }
        verify(jwtServiceMock).validateAndDecode("TOKEN")
      }
      "reject if jwt valid but id is missing" in new TestScope {
        when(jwtServiceMock.validateAndDecode(any[String]))
          .thenReturn(Future.successful(Some(JsObject("not_id" -> JsString("USER_ID")))))
        Get() ~> addCredentials(OAuth2BearerToken("TOKEN")) ~> route ~> check {
          rejection shouldBe a[AuthenticationFailedRejection]
        }
      }
      "reject if jwt validate fail failed" in new TestScope {
        when(jwtServiceMock.validateAndDecode(any[String])).thenReturn(Future.successful(None))
        Get() ~> addCredentials(OAuth2BearerToken("TOKEN")) ~> route ~> check {
          rejection shouldBe a[AuthenticationFailedRejection]
        }
      }
      "reject if missing auth header" in new TestScope {
        Get() ~> route ~> check {
          rejection shouldBe a[AuthenticationFailedRejection]
        }
        verify(jwtServiceMock, never).validateAndDecode(any[String])
      }
    }
  }
}
