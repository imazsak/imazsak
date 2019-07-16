package hu.ksisu.imazsak.prayer

import hu.ksisu.imazsak.prayer.PrayerService.CreatePrayerRequest
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

trait PrayerService[F[_]] {
  def createPrayer(data: CreatePrayerRequest)(implicit ctx: UserLogContext): F[Unit]
}

object PrayerService {
  case class CreatePrayerRequest(message: String, groupIds: Seq[String])
}
