package hu.ksisu.imazsak.prayer

import hu.ksisu.imazsak.Errors.Response
import hu.ksisu.imazsak.prayer.PrayerDao.{GroupPrayerListData, MyPrayerListData}
import hu.ksisu.imazsak.prayer.PrayerService.CreatePrayerRequest
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

trait PrayerService[F[_]] {
  def createPrayer(data: CreatePrayerRequest)(implicit ctx: UserLogContext): Response[F, Unit]
  def listMyPrayers()(implicit ctx: UserLogContext): Response[F, Seq[MyPrayerListData]]
  def listGroupPrayers(groupId: String)(implicit ctx: UserLogContext): Response[F, Seq[GroupPrayerListData]]
}

object PrayerService {
  case class CreatePrayerRequest(message: String, groupIds: Seq[String])
}
