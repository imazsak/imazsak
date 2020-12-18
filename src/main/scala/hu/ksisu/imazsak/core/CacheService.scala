package hu.ksisu.imazsak.core

import hu.ksisu.imazsak.Initable
import spray.json.JsonFormat

import scala.concurrent.duration.FiniteDuration

trait CacheService[F[_]] extends Initable[F] {
  def checkStatus(): F[Boolean]

  def findOrSet[T](key: String, ttl: Option[FiniteDuration] = None)(valueF: => F[T])(implicit
      format: JsonFormat[T]
  ): F[T]

  def remove(key: String): F[Unit]
}

object CacheService {
  def groupListByUserKey(userId: String) = s"group_list_$userId"

  def groupMemberListKey(groupId: String) = s"group_members_$groupId"

  def myPrayerListKey(userId: String) = s"my_prayer_list_$userId"

  def prayerDetailsKey(prayerId: String) = s"prayer_details_$prayerId"

  def prayerListByGroupKey(groupId: String) = s"group_prayer_list_$groupId"
}
