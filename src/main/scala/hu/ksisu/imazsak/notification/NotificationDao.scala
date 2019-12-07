package hu.ksisu.imazsak.notification

import hu.ksisu.imazsak.notification.NotificationDao.{CreateNotificationData, NotificationListData, NotificationMeta}
import reactivemongo.api.bson.{
  BSONDocument,
  BSONDocumentHandler,
  BSONDocumentReader,
  BSONDocumentWriter,
  Macros,
  document
}

trait NotificationDao[F[_]] {
  def createNotification(data: CreateNotificationData): F[String]
  def findByUser(userId: String): F[Seq[NotificationListData]]
  def findByUserOrderByDateDesc(userId: String, limit: Option[Int] = None): F[Seq[NotificationListData]]
  def updateMeta(id: String, meta: NotificationMeta): F[Unit]
  def setRead(ids: Seq[String], userId: String): F[Unit]
  def deleteByIds(ids: Seq[String], userId: Option[String] = None): F[Int]
}

object NotificationDao {
  case class NotificationMeta(isRead: Boolean, notificationType: Option[String])
  object NotificationMeta {
    val PRAYER_CREATED        = "PRAYER_CREATED"
    val PRAYER_CLOSE_FEEDBACK = "PRAYER_CLOSE_FEEDBACK"
  }
  case class CreateNotificationData(userId: String, message: String, createdAt: Long, meta: NotificationMeta)
  case class NotificationListData(id: String, message: String, createdAt: Long, meta: NotificationMeta)

  implicit val notificationMetaHandler: BSONDocumentHandler[NotificationMeta]   = Macros.handler[NotificationMeta]
  implicit val createNotiDataWriter: BSONDocumentWriter[CreateNotificationData] = Macros.writer[CreateNotificationData]
  implicit val notiListDataReader: BSONDocumentReader[NotificationListData]     = Macros.reader[NotificationListData]

  val notificationListDataProjector: Option[BSONDocument] = Option(
    document("id" -> 1, "message" -> 1, "createdAt" -> 1, "meta" -> 1)
  )
}
