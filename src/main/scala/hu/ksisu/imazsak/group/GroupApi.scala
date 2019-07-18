package hu.ksisu.imazsak.group

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import hu.ksisu.imazsak.Api
import hu.ksisu.imazsak.core.{AuthDirectives, JwtService}
import hu.ksisu.imazsak.group.GroupApi._
import hu.ksisu.imazsak.group.GroupDao._
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.Future

class GroupApi(implicit service: GroupService[Future], val jwtService: JwtService[Future])
    extends Api
    with AuthDirectives {

  def route(): Route = {
    path("groups") {
      get {
        userAuthAndTrace("Group_List") { implicit ctx =>
          onSuccess(service.listGroups()) { result =>
            complete(result)
          }
        }
      }
    }
  }
}

object GroupApi {
  implicit val groupListDataFormat: RootJsonFormat[GroupListData] = jsonFormat2(GroupListData)
}
