package hu.ksisu.imazsak.prayer

import hu.ksisu.imazsak.Errors.Response
import hu.ksisu.imazsak.prayer.PrayerDao.{GroupPrayerListData, MyPrayerListData}
import hu.ksisu.imazsak.prayer.PrayerService.{CreatePrayerRequest, Next10PrayerListData, PrayerCloseRequest}
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

trait PrayerService[F[_]] {
  def createPrayer(data: CreatePrayerRequest)(implicit ctx: UserLogContext): Response[F, Unit]
  def listMyPrayers()(implicit ctx: UserLogContext): Response[F, Seq[MyPrayerListData]]
  def listGroupPrayers(groupId: String)(implicit ctx: UserLogContext): Response[F, Seq[GroupPrayerListData]]
  def pray(groupId: String, prayerId: String)(implicit ctx: UserLogContext): Response[F, Unit]
  def next10(groupIds: Seq[String])(implicit ctx: UserLogContext): Response[F, Seq[Next10PrayerListData]]
  def close(data: PrayerCloseRequest)(implicit ctx: UserLogContext): Response[F, Unit]
}

object PrayerService {
  import spray.json._
  import DefaultJsonProtocol._

  case class CreatePrayerRequest(message: String, groupIds: Seq[String])
  case class Next10PrayerListData(id: String, userId: String, groupId: String, message: String)
  case class PrayerCloseRequest(id: String, message: Option[String])

  case class PrayerCreatedNotificationData(prayerId: String, userName: Option[String], message: String, groupIds: Seq[String])
  case class PrayerCloseFeedbackNotificationData(userName: Option[String], message: String, feedback: String)
  implicit val formatPrayerCloseFeedbackNotificationMessage: RootJsonFormat[PrayerCloseFeedbackNotificationData] =
    jsonFormat3(PrayerCloseFeedbackNotificationData)
  implicit val formatPrayerCreatedNotificationData: RootJsonFormat[PrayerCreatedNotificationData] =
    jsonFormat4(PrayerCreatedNotificationData)
}
