package hu.ksisu.imazsak.core

object Errors {
  sealed abstract class ConfigError(msg: String) extends Throwable(msg)
  case class WrongConfig(msg: String)            extends ConfigError(msg)

  sealed abstract class JwtError(msg: String) extends Throwable(msg)
  case class InvalidJwt()                     extends JwtError("Invalid JWT")
}
