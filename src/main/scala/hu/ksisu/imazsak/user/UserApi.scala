package hu.ksisu.imazsak.user

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.effect.IO
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.Errors._
import hu.ksisu.imazsak.core.{AuthDirectives, JwtService}
import hu.ksisu.imazsak.user.UserDao.UserAdminListData
import hu.ksisu.imazsak.util.LoggerUtil.Logger
import spray.json.DefaultJsonProtocol._
import spray.json._
import hu.ksisu.imazsak.user.UserApi._

class UserApi(implicit service: UserService[IO], val jwtService: JwtService[IO]) extends Api with AuthDirectives {
  implicit val logger = new Logger("UserApi")

  def route(): Route = {
    path("groups" / Segment / "prayers") { groupId =>
      userAuthAndTrace("Users_ListGroupMembers") { implicit ctx =>
        service.listGroupUsers(groupId).toComplete
      }
    }
  }
}

object UserApi {
  implicit val userAdminListDataFormat: RootJsonFormat[UserAdminListData] = jsonFormat2(UserAdminListData)
}
