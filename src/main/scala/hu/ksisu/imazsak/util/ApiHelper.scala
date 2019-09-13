package hu.ksisu.imazsak.util

object ApiHelper {
  import spray.json.DefaultJsonProtocol._
  import spray.json._

  case class Ids(ids: Seq[String])
  case class Token(token: String)

  implicit val idsFormat: RootJsonFormat[Ids]     = jsonFormat1(Ids)
  implicit val tokenFormat: RootJsonFormat[Token] = jsonFormat1(Token)
}
