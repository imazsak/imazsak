package hu.ksisu.imazsak

import cats.effect.{ContextShift, IO}
import hu.ksisu.imazsak.admin.{AdminService, AdminServiceImpl}
import hu.ksisu.imazsak.core._
import hu.ksisu.imazsak.core.config.{ServerConfig, ServerConfigImpl}
import hu.ksisu.imazsak.core.dao._
import hu.ksisu.imazsak.core.healthcheck.{HealthCheckService, HealthCheckServiceImpl}
import hu.ksisu.imazsak.core.impl.JwtServiceImpl
import hu.ksisu.imazsak.feedback.{FeedbackDao, FeedbackDaoImpl, FeedbackService, FeedbackServiceImpl}
import hu.ksisu.imazsak.group.{GroupDao, GroupDaoImpl, GroupService, GroupServiceImpl}
import hu.ksisu.imazsak.notification._
import hu.ksisu.imazsak.prayer.{PrayerDao, PrayerDaoImpl, PrayerService, PrayerServiceImpl}
import hu.ksisu.imazsak.user._
import hu.ksisu.imazsak.util._
import reactivemongo.api.MongoDriver

import scala.concurrent.ExecutionContext

class ItTestServices(implicit ec: ExecutionContext) extends Services[IO] {
  implicit lazy val cs: ContextShift[IO]                     = IO.contextShift(ec)
  override implicit lazy val configService: ServerConfig[IO] = new ServerConfigImpl[IO]
  import configService._
  override implicit lazy val healthCheckService: HealthCheckService[IO] = new HealthCheckServiceImpl[IO]
  implicit lazy val mongoDriver                                         = new MongoDriver()
  override implicit lazy val databaseService: MongoDatabaseService[IO]  = new MongoDatabaseServiceImpl()

  override implicit lazy val idGenerator: IdGenerator      = new IdGeneratorCounterImpl
  override implicit lazy val dateTimeService: DateTimeUtil = new DateTimeUtilCounterImpl
  implicit lazy val httpWrapper: HttpWrapper[IO]           = null

  override implicit lazy val tracerService: TracerService[IO]        = new TracerService[IO]()
  override implicit lazy val amqpService: AmqpService[IO]            = null
  override implicit lazy val jwtService: JwtServiceImpl[IO]          = new JwtServiceImpl[IO]()
  override implicit lazy val userDao: UserDao[IO]                    = new UserDaoImpl()
  override implicit lazy val meService: MeService[IO]                = new MeServiceImpl[IO]()
  override implicit lazy val groupDao: GroupDao[IO]                  = new GroupDaoImpl()
  override implicit lazy val groupService: GroupService[IO]          = new GroupServiceImpl()
  implicit lazy val prayerDao: PrayerDao[IO]                         = new PrayerDaoImpl()
  implicit lazy val prayerService: PrayerService[IO]                 = new PrayerServiceImpl[IO]()
  implicit lazy val adminService: AdminService[IO]                   = new AdminServiceImpl[IO]()
  implicit lazy val fileStoreService: FileStoreService[IO]           = null
  implicit lazy val feedbackDao: FeedbackDao[IO]                     = new FeedbackDaoImpl()
  implicit lazy val feedbackService: FeedbackService[IO]             = new FeedbackServiceImpl[IO]()
  override implicit val notificationDao: NotificationDao[IO]         = new NotificationDaoImpl()
  override implicit val notificationService: NotificationService[IO] = new NotificationServiceImpl[IO]()
  override implicit val userService: UserService[IO]                 = new UserServiceImpl[IO]()
}
