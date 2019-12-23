package hu.ksisu.imazsak

import akka.actor.ActorSystem
import cats.MonadError
import cats.effect.{ContextShift, IO}
import hu.ksisu.imazsak.admin.{AdminService, AdminServiceImpl}
import hu.ksisu.imazsak.core._
import hu.ksisu.imazsak.core.config.{ServerConfig, ServerConfigImpl}
import hu.ksisu.imazsak.core.dao._
import hu.ksisu.imazsak.core.healthcheck.{HealthCheckService, HealthCheckServiceImpl}
import hu.ksisu.imazsak.core.impl._
import hu.ksisu.imazsak.feedback.{FeedbackDao, FeedbackDaoImpl, FeedbackService, FeedbackServiceImpl}
import hu.ksisu.imazsak.group.{GroupDao, GroupDaoImpl, GroupService, GroupServiceImpl}
import hu.ksisu.imazsak.notification._
import hu.ksisu.imazsak.prayer.{PrayerDao, PrayerDaoImpl, PrayerService, PrayerServiceImpl}
import hu.ksisu.imazsak.token.{TokenDao, TokenDaoImpl, TokenService, TokenServiceImpl}
import hu.ksisu.imazsak.user._
import hu.ksisu.imazsak.util._
import org.slf4j.Logger
import reactivemongo.api.AsyncDriver

import scala.concurrent.ExecutionContext

trait Services[F[_]] {
  implicit val configService: ServerConfig[F]
  implicit val healthCheckService: HealthCheckService[F]
  implicit val databaseService: MongoDatabaseService[F]
  implicit val idGenerator: IdGenerator
  implicit val dateTimeService: DateTimeUtil
  implicit val tracerService: TracerService[F]
  implicit val amqpService: AmqpService[F]
  implicit val jwtService: JwtService[F]
  implicit val userDao: UserDao[F]
  implicit val meService: MeService[F]
  implicit val groupDao: GroupDao[F]
  implicit val groupService: GroupService[F]
  implicit val prayerDao: PrayerDao[F]
  implicit val prayerService: PrayerService[F]
  implicit val adminService: AdminService[F]
  implicit val fileStoreService: FileStoreService[F]
  implicit val feedbackDao: FeedbackDao[F]
  implicit val feedbackService: FeedbackService[F]
  implicit val notificationDao: NotificationDao[F]
  implicit val notificationService: NotificationService[F]
  implicit val tokenDao: TokenDao[F]
  implicit val tokenService: TokenService[F]
  implicit val authHookService: AuthHookService[F]
  implicit val pushNotificationService: PushNotificationService[F]
  implicit val redisService: CacheService[F]

  def init()(implicit logger: Logger, ev: MonadError[F, Throwable]): F[Unit] = {
    import Initable._
    import cats.syntax.flatMap._
    import cats.syntax.functor._

    logger.info("Init services")
    for {
      _ <- initialize(configService, "config")
      _ <- initialize(tracerService, "tracer")
      _ <- initialize(databaseService, "database")
      _ <- initialize(fileStoreService, "filestore")
      _ <- initialize(amqpService, "ampq")
      _ <- initialize(tokenService, "token")
      _ <- initialize(notificationService, "notification")
      _ <- initialize(pushNotificationService, "push")
      _ <- initialize(redisService, "redis")
    } yield ()
  }
}

class RealServices(
    implicit ec: ExecutionContext,
    actorSystem: ActorSystem,
    cs: ContextShift[IO]
) extends Services[IO] {

  implicit lazy val configService: ServerConfig[IO] = new ServerConfigImpl[IO]
  import configService._

  implicit lazy val redisService: CacheService[IO]                       = new RedisServiceImpl()
  implicit lazy val healthCheckService: HealthCheckService[IO]           = new HealthCheckServiceImpl[IO]
  implicit lazy val mongoDriver: AsyncDriver                             = new AsyncDriver()
  implicit lazy val databaseService: MongoDatabaseService[IO]            = new MongoDatabaseServiceImpl()
  implicit lazy val httpWrapper: HttpWrapper[IO]                         = new AkkaHttpWrapper()
  implicit lazy val idGenerator: IdGenerator                             = new IdGeneratorImpl
  implicit lazy val dateTimeService: DateTimeUtil                        = new DateTimeUtilImpl
  implicit lazy val tracerService: TracerService[IO]                     = new TracerService[IO]()
  implicit lazy val amqpService: AmqpService[IO]                         = new AmqpServiceImpl[IO]()
  implicit lazy val jwtService: JwtServiceImpl[IO]                       = new JwtServiceImpl[IO]()
  implicit lazy val userDao: UserDao[IO]                                 = new UserDaoImpl()
  implicit lazy val meService: MeService[IO]                             = new MeServiceImpl[IO]()
  implicit lazy val groupDao: GroupDao[IO]                               = new GroupDaoImpl()
  implicit lazy val groupService: GroupService[IO]                       = new GroupServiceImpl()
  implicit lazy val prayerDao: PrayerDao[IO]                             = new PrayerDaoImpl()
  implicit lazy val prayerService: PrayerService[IO]                     = new PrayerServiceImpl[IO]()
  implicit lazy val adminService: AdminService[IO]                       = new AdminServiceImpl[IO]()
  implicit lazy val fileStoreService: FileStoreService[IO]               = new S3FileStoreService()
  implicit lazy val feedbackDao: FeedbackDao[IO]                         = new FeedbackDaoImpl()
  implicit lazy val feedbackService: FeedbackService[IO]                 = new FeedbackServiceImpl[IO]()
  implicit lazy val notificationDao: NotificationDao[IO]                 = new NotificationDaoImpl()
  implicit lazy val notificationService: NotificationService[IO]         = new NotificationServiceImpl[IO]()
  implicit lazy val tokenDao: TokenDao[IO]                               = new TokenDaoImpl()
  implicit lazy val tokenService: TokenService[IO]                       = new TokenServiceImpl[IO]()
  implicit lazy val authHookService: AuthHookService[IO]                 = new AuthHookServiceImpl[IO]()
  implicit lazy val pushNotificationService: PushNotificationService[IO] = new PushNotificationServiceImpl()
}
