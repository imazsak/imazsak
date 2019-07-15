package hu.ksisu.imazsak

import hu.ksisu.imazsak.core._
import hu.ksisu.imazsak.core.config.{ServerConfig, ServerConfigImpl}
import hu.ksisu.imazsak.core.dao.{MongoDatabaseService, MongoDatabaseServiceImpl, UserDao, UserDaoImpl}
import hu.ksisu.imazsak.core.healthcheck.{HealthCheckService, HealthCheckServiceImpl}
import hu.ksisu.imazsak.core.impl.JwtServiceImpl
import hu.ksisu.imazsak.me.{MeService, MeServiceImpl}
import hu.ksisu.imazsak.util._
import reactivemongo.api.MongoDriver

import scala.concurrent.{ExecutionContext, Future}

class ItTestServices(implicit ec: ExecutionContext) extends Services[Future] {
  import cats.instances.future._
  override implicit lazy val configService: ServerConfig[Future] = new ServerConfigImpl[Future]
  import configService._
  override implicit lazy val healthCheckService: HealthCheckService[Future] = new HealthCheckServiceImpl[Future]
  implicit lazy val mongoDriver                                             = new MongoDriver()
  override implicit lazy val databaseService: MongoDatabaseService[Future]  = new MongoDatabaseServiceImpl()

  override implicit lazy val idGenerator: IdGenerator[Future]      = new IdGeneratorCounterImpl[Future]
  override implicit lazy val dateTimeService: DateTimeUtil[Future] = new DateTimeUtilCounterImpl[Future]
  implicit lazy val httpWrapper: HttpWrapper[Future]               = null

  override implicit val tracerService: TracerService[Future] = new TracerService[Future]()
  override implicit val amqpService: AmqpService[Future]     = null
  override implicit val jwtService: JwtServiceImpl[Future]   = new JwtServiceImpl[Future]()
  override implicit val userDao: UserDao[Future]             = new UserDaoImpl()
  override implicit val meService: MeService[Future]         = new MeServiceImpl[Future]()
}
