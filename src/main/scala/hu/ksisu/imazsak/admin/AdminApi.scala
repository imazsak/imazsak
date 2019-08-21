package hu.ksisu.imazsak.admin

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.effect.IO
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.Errors._
import hu.ksisu.imazsak.admin.AdminApi._
import hu.ksisu.imazsak.admin.AdminService.{AddUserToGroupRequest, CreateGroupRequest}
import hu.ksisu.imazsak.core.{AuthDirectives, JwtService}
import hu.ksisu.imazsak.group.GroupDao.{GroupAdminListData, GroupMember}
import hu.ksisu.imazsak.user.UserDao.UserAdminListData
import hu.ksisu.imazsak.util.LoggerUtil.Logger
import spray.json.DefaultJsonProtocol._
import spray.json._

class AdminApi(implicit service: AdminService[IO], val jwtService: JwtService[IO]) extends Api with AuthDirectives {
  implicit val logger = new Logger("AdminApi")

  def route(): Route = {
    pathPrefix("admin") {
      path("groups") {
        get {
          adminAuthAndTrace("Admin_ListGroups") { implicit ctx =>
            service.listAllGroup().toComplete
          }
        } ~ post {
          entity(as[CreateGroupRequest]) { data =>
            adminAuthAndTrace("Admin_CreateGroup") { implicit ctx =>
              service.createGroup(data).toComplete
            }
          }
        }
      } ~ path("groups" / "add-user") {
        post {
          entity(as[AddUserToGroupRequest]) { data =>
            adminAuthAndTrace("Admin_AddUserGroups") { implicit ctx =>
              service.addUserToGroup(data).toComplete
            }
          }
        }
      } ~ path("users") {
        get {
          adminAuthAndTrace("Admin_ListUsers") { implicit ctx =>
            service.listAllUser().toComplete
          }
        }
      }
    }
  }
}

object AdminApi {
  implicit val groupMemberFormat: RootJsonFormat[GroupMember]                     = jsonFormat1(GroupMember)
  implicit val groupAdminListDataFormat: RootJsonFormat[GroupAdminListData]       = jsonFormat3(GroupAdminListData)
  implicit val userAdminListDataaFormat: RootJsonFormat[UserAdminListData]        = jsonFormat2(UserAdminListData)
  implicit val addUserToGroupRequestFormat: RootJsonFormat[AddUserToGroupRequest] = jsonFormat2(AddUserToGroupRequest)
  implicit val createGroupRequestFormat: RootJsonFormat[CreateGroupRequest]       = jsonFormat2(CreateGroupRequest)
}
