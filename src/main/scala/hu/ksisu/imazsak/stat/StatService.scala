package hu.ksisu.imazsak.stat

import hu.ksisu.imazsak.Errors.Response
import hu.ksisu.imazsak.Initable
import hu.ksisu.imazsak.prayer.PrayerService.{CreatePrayerRequest, PrayerCloseRequest}
import hu.ksisu.imazsak.util.LoggerUtil.UserLogContext

trait StatService[F[_]] extends Initable[F] {
  def prayerCreated(data: CreatePrayerRequest)(implicit ctx: UserLogContext): Response[F, Unit]
  def prayerClosed(data: PrayerCloseRequest)(implicit ctx: UserLogContext): Response[F, Unit]
  def prayed(prayerId: String)(implicit ctx: UserLogContext): Response[F, Unit]
  def joinedToGroup(groupId: String)(implicit ctx: UserLogContext): Response[F, Unit]
}
