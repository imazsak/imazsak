package hu.ksisu.imazsak.group

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.effect.IO
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.Errors._
import hu.ksisu.imazsak.core.{AuthDirectives, JwtService}
import hu.ksisu.imazsak.group.GroupApi._
import hu.ksisu.imazsak.group.GroupDao._
import hu.ksisu.imazsak.util.ApiHelper._
import hu.ksisu.imazsak.util.LoggerUtil.Logger
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

class GroupApi(implicit service: GroupService[IO], val jwtService: JwtService[IO]) extends Api with AuthDirectives {
  implicit val logger = new Logger("GroupApi")

  def route(): Route = {
    path("groups") {
      get {
        userAuthAndTrace("Group_List") { implicit ctx =>
          service.listGroups().toComplete
        }
      }
    } ~
      path("groups" / Segment / "create-join-token") { groupId =>
        post {
          userAuthAndTrace("Group_CreateToken") { implicit ctx =>
            service.createJoinToken(groupId).map(Token).toComplete
          }
        }
      } ~
      path("groups" / Segment / "join") { groupId =>
        post {
          entity(as[Token]) { data =>
            userAuthAndTrace("Group_JoinWithToken") { implicit ctx =>
              service.joinToGroup(groupId, data.token).toComplete
            }
          }
        }
      }
  }
}

object GroupApi {
  implicit val groupListDataFormat: RootJsonFormat[GroupListData] = jsonFormat2(GroupListData)
}
