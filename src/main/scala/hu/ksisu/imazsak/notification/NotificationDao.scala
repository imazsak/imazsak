package hu.ksisu.imazsak.notification

import hu.ksisu.imazsak.notification.NotificationDao.{CreateNotificationData, NotificationListData, NotificationMeta}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, Macros, document}

trait NotificationDao[F[_]] {
  def createNotification(data: CreateNotificationData): F[String]
  def findByUser(userId: String): F[Seq[NotificationListData]]
  def updateMeta(id: String, meta: NotificationMeta): F[Unit]
  def delete(id: String): F[Unit]
}

object NotificationDao {
  case class NotificationMeta(isRead: Boolean, notificationType: Option[String])
  case class CreateNotificationData(userId: String, message: String, createdAt: Long, meta: NotificationMeta)
  case class NotificationListData(id: String, message: String, createdAt: Long, meta: NotificationMeta)

  implicit def notificationMetaReader: BSONDocumentReader[NotificationMeta]     = Macros.reader[NotificationMeta]
  implicit def notificationMetaWriter: BSONDocumentWriter[NotificationMeta]     = Macros.writer[NotificationMeta]
  implicit def createNotiDataWriter: BSONDocumentWriter[CreateNotificationData] = Macros.writer[CreateNotificationData]
  implicit def notiListDataReader: BSONDocumentReader[NotificationListData]     = Macros.reader[NotificationListData]

  val notificationListDataProjector: Option[BSONDocument] = Option(
    document("id" -> 1, "message" -> 1, "createdAt" -> 1, "meta" -> 1)
  )
}
