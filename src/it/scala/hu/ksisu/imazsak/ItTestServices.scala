package hu.ksisu.imazsak

import hu.ksisu.imazsak.core._
import hu.ksisu.imazsak.core.config.{ServerConfig, ServerConfigImpl}
import hu.ksisu.imazsak.core.dao._
import hu.ksisu.imazsak.core.healthcheck.{HealthCheckService, HealthCheckServiceImpl}
import hu.ksisu.imazsak.core.impl.JwtServiceImpl
import hu.ksisu.imazsak.group.{GroupService, GroupServiceImpl}
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

  override implicit lazy val tracerService: TracerService[Future] = new TracerService[Future]()
  override implicit lazy val amqpService: AmqpService[Future]     = null
  override implicit lazy val jwtService: JwtServiceImpl[Future]   = new JwtServiceImpl[Future]()
  override implicit lazy val userDao: UserDao[Future]             = new UserDaoImpl()
  override implicit lazy val meService: MeService[Future]         = new MeServiceImpl[Future]()
  override implicit lazy val groupDao: GroupDao[Future]           = new GroupDaoImpl()
  override implicit lazy val groupService: GroupService[Future]   = new GroupServiceImpl()
}
