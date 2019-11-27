package hu.ksisu.imazsak.feedback

import cats.MonadError
import cats.data.EitherT
import hu.ksisu.imazsak.Errors.{AppError, Response}
import hu.ksisu.imazsak.feedback.FeedbackDao.CreateFeedbackData
import hu.ksisu.imazsak.feedback.FeedbackService.CreateFeedbackRequest
import hu.ksisu.imazsak.util.DateTimeUtil
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

class FeedbackServiceImpl[F[_]: MonadError[*[_], Throwable]](
    implicit feedbackDao: FeedbackDao[F],
    date: DateTimeUtil
) extends FeedbackService[F] {

  override def createFeedback(data: CreateFeedbackRequest)(implicit ctx: UserLogContext): Response[F, Unit] = {
    val model = CreateFeedbackData(
      ctx.userId,
      data.message,
      date.getCurrentTimeMillis
    )
    EitherT.right[AppError](feedbackDao.create(model)).map(_ => ())
  }

}
