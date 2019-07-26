package hu.ksisu.imazsak.feedback

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.Errors._
import hu.ksisu.imazsak.core.{AuthDirectives, JwtService}
import hu.ksisu.imazsak.feedback.FeedbackApi._
import hu.ksisu.imazsak.feedback.FeedbackService.CreateFeedbackRequest
import hu.ksisu.imazsak.util.LoggerUtil.Logger
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.Future

class FeedbackApi(implicit service: FeedbackService[Future], val jwtService: JwtService[Future])
    extends Api
    with AuthDirectives {
  implicit val logger = new Logger("FeedbackApi")

  def route(): Route = {
    path("feedback") {
      post {
        userAuthAndTrace("Feedback_create") { implicit ctx =>
          entity(as[CreateFeedbackRequest]) { data =>
            service.createFeedback(data).toComplete
          }
        }
      }
    }
  }

}

object FeedbackApi {
  implicit val createFeedbackRequestFormat: RootJsonFormat[CreateFeedbackRequest] = jsonFormat1(CreateFeedbackRequest)
}
