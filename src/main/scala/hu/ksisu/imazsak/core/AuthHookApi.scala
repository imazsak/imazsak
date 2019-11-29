package hu.ksisu.imazsak.core

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, HttpChallenge, HttpCredentials}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.effect.IO
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.core.AuthHookApi.{HookDeleteData, HookUserData}
import spray.json.{JsObject, JsTrue, RootJsonFormat}

import scala.concurrent.Future

class AuthHookApi(implicit authHookService: AuthHookService[IO]) extends Api {

  private def tutelarAuthenticator(credentials: Option[HttpCredentials]): Future[AuthenticationResult[Unit]] = {
    credentials match {
      case Some(BasicHttpCredentials(_, token)) if authHookService.verifyAuthService(token).unsafeRunSync() =>
        Future.successful(Right({}))
      case _ =>
        Future.successful(Left(HttpChallenge("", Some(""))))
    }
  }

  override def route(): Route = {
    pathPrefix("core" / "hooks") {
      authenticateOrRejectWithChallenge[Unit](tutelarAuthenticator) { _ =>
        post {
          entity(as[HookUserData]) { data =>
            path("register") {
              complete(JsObject.empty)
            } ~ path("login") {
              val isAdmin = authHookService.isAdmin(data.id).unsafeRunSync()
              if (isAdmin) {
                complete(JsObject("isAdmin" -> JsTrue))
              } else {
                complete(JsObject.empty)
              }
            } ~ path("modify") {
              complete(JsObject.empty)
            } ~ path("link") {
              complete(JsObject.empty)
            } ~ path("unlink") {
              complete(JsObject.empty)
            }
          } ~
            entity(as[HookDeleteData]) { _ =>
              {
                path("delete") {
                  complete(JsObject.empty)
                }
              }
            }
        }
      }
    }
  }
}

object AuthHookApi {
  case class HookUserData(id: String, externalId: String, authType: String, data: Option[JsObject])
  case class HookDeleteData(id: String)

  import spray.json.DefaultJsonProtocol._
  implicit val hookRequestDataFormat: RootJsonFormat[HookUserData]  = jsonFormat4(HookUserData)
  implicit val hookDeleteDataFormat: RootJsonFormat[HookDeleteData] = jsonFormat1(HookDeleteData)
}
