package hu.ksisu.imazsak.feedback

import hu.ksisu.imazsak.Errors._
import hu.ksisu.imazsak.feedback.FeedbackService.CreateFeedbackRequest
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

trait FeedbackService[F[_]] {
  def createFeedback(data: CreateFeedbackRequest)(implicit ctx: UserLogContext): Response[F, Unit]
}

object FeedbackService {
  case class CreateFeedbackRequest(message: String)
}
