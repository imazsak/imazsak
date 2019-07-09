package hu.ksisu.imazsak.core.config
import cats.MonadError
import com.typesafe.config.{Config, ConfigFactory}
import hu.ksisu.imazsak.Initable
import hu.ksisu.imazsak.core.AmqpService.AmqpConfig
import hu.ksisu.imazsak.core.Errors.WrongConfig
import hu.ksisu.imazsak.core.TracerService.TracerServiceConfig
import hu.ksisu.imazsak.core.dao.MongoDatabaseService.MongoConfig
import hu.ksisu.imazsak.core.impl.JwtServiceImpl.JwtConfig

trait ServerConfig[F[_]] extends Initable[F] {
  def getEnabledModules: Seq[String]

  implicit def getMongoConfig: MongoConfig

  implicit def getTracerServiceConfig: TracerServiceConfig

  implicit def getJwtConfig: JwtConfig

  implicit def getAmqpConfig: AmqpConfig
}

class ServerConfigImpl[F[_]: MonadError[?[_], Throwable]]() extends ServerConfig[F] {
  import cats.syntax.applicative._
  private lazy val conf: Config = ConfigFactory.load

  lazy val getEnabledModules: Seq[String] = {
    conf
      .getString("modulesEnabled")
      .split(',')
      .map(_.trim.toLowerCase)
      .filterNot(_.isEmpty)
      .toSeq
  }

  override def init: F[Unit] = {
    import hu.ksisu.imazsak.util.ApplicativeErrorSyntax._
    if (conf.isEmpty) WrongConfig("Config is empty!").raise
    else ().pure
  }

  override implicit def getMongoConfig: MongoConfig = {
    val config = conf.getConfig("database.mongo")
    MongoConfig(
      config.getString("uri")
    )
  }

  override implicit def getTracerServiceConfig: TracerServiceConfig = {
    val config = conf.getConfig("tracer")
    TracerServiceConfig(
      config.getString("client")
    )
  }

  override implicit def getJwtConfig: JwtConfig = {
    val config = conf.getConfig(s"jwt")
    JwtConfig(
      config.getString("algorithm"),
      config.getString("secret")
    )
  }

  override implicit def getAmqpConfig: AmqpConfig = {
    val config = conf.getConfig("amqp")
    AmqpConfig(
      config.getString("uri")
    )
  }
}
