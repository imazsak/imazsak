package hu.ksisu.imazsak

import cats.effect.{ContextShift, IO}
import hu.ksisu.imazsak.admin.{AdminService, AdminServiceImpl}
import hu.ksisu.imazsak.core._
import hu.ksisu.imazsak.core.config.{ServerConfig, ServerConfigImpl}
import hu.ksisu.imazsak.core.dao._
import hu.ksisu.imazsak.core.healthcheck.{HealthCheckService, HealthCheckServiceImpl}
import hu.ksisu.imazsak.core.impl.{AuthHookServiceImpl, JwtServiceImpl}
import hu.ksisu.imazsak.feedback.{FeedbackDao, FeedbackDaoImpl, FeedbackService, FeedbackServiceImpl}
import hu.ksisu.imazsak.group.{GroupDao, GroupDaoImpl, GroupService, GroupServiceImpl}
import hu.ksisu.imazsak.notification._
import hu.ksisu.imazsak.prayer.{PrayerDao, PrayerDaoImpl, PrayerService, PrayerServiceImpl}
import hu.ksisu.imazsak.token.{TokenDao, TokenDaoImpl, TokenService, TokenServiceImpl}
import hu.ksisu.imazsak.user._
import hu.ksisu.imazsak.util._
import reactivemongo.api.AsyncDriver

import scala.concurrent.ExecutionContext

class ItTestServices(implicit ec: ExecutionContext) extends Services[IO] {
  implicit lazy val cs: ContextShift[IO]            = IO.contextShift(ec)
  implicit lazy val configService: ServerConfig[IO] = new ServerConfigImpl[IO]
  import configService._
  implicit lazy val healthCheckService: HealthCheckService[IO] = new HealthCheckServiceImpl[IO]
  implicit lazy val mongoDriver                                = new AsyncDriver()
  implicit lazy val databaseService: MongoDatabaseService[IO]  = new MongoDatabaseServiceImpl()

  implicit lazy val idGenerator: IdGenerator      = new IdGeneratorCounterImpl
  implicit lazy val dateTimeService: DateTimeUtil = new DateTimeUtilCounterImpl
  implicit lazy val httpWrapper: HttpWrapper[IO]  = null

  implicit lazy val tracerService: TracerService[IO]                     = new TracerService[IO]()
  implicit lazy val amqpService: AmqpService[IO]                         = null
  implicit lazy val jwtService: JwtServiceImpl[IO]                       = new JwtServiceImpl[IO]()
  implicit lazy val userDao: UserDao[IO]                                 = new UserDaoImpl()
  implicit lazy val meService: MeService[IO]                             = new MeServiceImpl[IO]()
  implicit lazy val groupDao: GroupDao[IO]                               = new GroupDaoImpl()
  implicit lazy val groupService: GroupService[IO]                       = new GroupServiceImpl()
  implicit lazy val prayerDao: PrayerDao[IO]                             = new PrayerDaoImpl()
  implicit lazy val prayerService: PrayerService[IO]                     = new PrayerServiceImpl[IO]()
  implicit lazy val adminService: AdminService[IO]                       = new AdminServiceImpl[IO]()
  implicit lazy val fileStoreService: FileStoreService[IO]               = null
  implicit lazy val feedbackDao: FeedbackDao[IO]                         = new FeedbackDaoImpl()
  implicit lazy val feedbackService: FeedbackService[IO]                 = new FeedbackServiceImpl[IO]()
  implicit lazy val notificationDao: NotificationDao[IO]                 = new NotificationDaoImpl()
  implicit lazy val notificationService: NotificationService[IO]         = null
  implicit lazy val userService: UserService[IO]                         = new UserServiceImpl[IO]()
  implicit lazy val tokenDao: TokenDao[IO]                               = new TokenDaoImpl()
  implicit lazy val tokenService: TokenService[IO]                       = new TokenServiceImpl[IO]()
  implicit lazy val authHookService: AuthHookService[IO]                 = new AuthHookServiceImpl[IO]()
  implicit lazy val pushNotificationService: PushNotificationService[IO] = new PushNotificationServiceImpl()
  override implicit val redisService: RedisService[IO]                   = null
}
