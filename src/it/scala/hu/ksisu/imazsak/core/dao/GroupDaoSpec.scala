package hu.ksisu.imazsak.core.dao

import cats.effect.IO
import hu.ksisu.imazsak.AwaitUtil
import hu.ksisu.imazsak.group.GroupDao
import hu.ksisu.imazsak.group.GroupDao.{CreateGroupData, GroupAdminListData, GroupListData, GroupMember}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import reactivemongo.api.bson._
import reactivemongo.api.bson.collection.BSONCollection

import scala.concurrent.ExecutionContext.Implicits.global

trait GroupDaoSpec {
  self: AnyWordSpecLike with Matchers with AwaitUtil =>

  protected val groupDao: GroupDao[IO]
  protected val groupCollection: BSONCollection

  def groupDaoTests(): Unit = {
    "GroupDao" when {
      "#findGroupsByUser" in {
        val group1 = BSONDocument(
          "id"   -> BSONString("group_1"),
          "name" -> BSONString("Group #1"),
          "members" -> BSONArray(
            BSONDocument("id" -> BSONString("user_1")),
            BSONDocument("id" -> BSONString("user_2"))
          )
        )
        val group2 = BSONDocument(
          "id"   -> BSONString("group_2"),
          "name" -> BSONString("Group #2"),
          "members" -> BSONArray(
            BSONDocument("id" -> BSONString("user_1")),
            BSONDocument("id" -> BSONString("user_3"))
          )
        )
        await(groupCollection.insert.one(group1))
        await(groupCollection.insert.one(group2))

        groupDao.findGroupsByUser("user_1").unsafeRunSync() shouldEqual Seq(
          GroupListData("group_1", "Group #1"),
          GroupListData("group_2", "Group #2")
        )
        groupDao.findGroupsByUser("user_2").unsafeRunSync() shouldEqual Seq(GroupListData("group_1", "Group #1"))
        groupDao.findGroupsByUser("user_3").unsafeRunSync() shouldEqual Seq(GroupListData("group_2", "Group #2"))
      }
      "#findGroupsByName" in {
        val group1 = BSONDocument(
          "id"   -> BSONString("group_1"),
          "name" -> BSONString("Group #1"),
          "members" -> BSONArray(
            BSONDocument("id" -> BSONString("user_1")),
            BSONDocument("id" -> BSONString("user_2"))
          )
        )
        val group2 = BSONDocument(
          "id"   -> BSONString("group_2"),
          "name" -> BSONString("Group #2"),
          "members" -> BSONArray(
            BSONDocument("id" -> BSONString("user_1")),
            BSONDocument("id" -> BSONString("user_3"))
          )
        )
        await(groupCollection.insert.one(group1))
        await(groupCollection.insert.one(group2))

        groupDao.findGroupByName("Group #1").value.unsafeRunSync() shouldEqual Some(
          GroupListData("group_1", "Group #1")
        )
        groupDao.findGroupByName("Group").value.unsafeRunSync() shouldEqual None
      }
      "#findMembersByGroupId" in {
        val group1 = BSONDocument(
          "id"   -> BSONString("group_1"),
          "name" -> BSONString("Group #1"),
          "members" -> BSONArray(
            BSONDocument("id" -> BSONString("user_1")),
            BSONDocument("id" -> BSONString("user_2"))
          )
        )
        val group2 = BSONDocument(
          "id"   -> BSONString("group_2"),
          "name" -> BSONString("Group #2"),
          "members" -> BSONArray(
            BSONDocument("id" -> BSONString("user_1")),
            BSONDocument("id" -> BSONString("user_3"))
          )
        )
        await(groupCollection.insert.one(group1))
        await(groupCollection.insert.one(group2))

        groupDao.findMembersByGroupId("group_1").unsafeRunSync() shouldEqual Seq(
          GroupMember("user_1"),
          GroupMember("user_2")
        )
        groupDao.findMembersByGroupId("group_2").unsafeRunSync() shouldEqual Seq(
          GroupMember("user_1"),
          GroupMember("user_3")
        )
      }
      "findMembersByUserId" in {
        val group1 = BSONDocument(
          "id"   -> BSONString("group_1"),
          "name" -> BSONString("Group #1"),
          "members" -> BSONArray(
            BSONDocument("id" -> BSONString("user_1")),
            BSONDocument("id" -> BSONString("user_2"))
          )
        )
        val group2 = BSONDocument(
          "id"   -> BSONString("group_2"),
          "name" -> BSONString("Group #2"),
          "members" -> BSONArray(
            BSONDocument("id" -> BSONString("user_1")),
            BSONDocument("id" -> BSONString("user_3"))
          )
        )
        await(groupCollection.insert.one(group1))
        await(groupCollection.insert.one(group2))

        groupDao.findMembersByUserId("user_1").unsafeRunSync().toSet shouldEqual Set(
          GroupMember("user_1"),
          GroupMember("user_2"),
          GroupMember("user_3")
        )
        groupDao.findMembersByUserId("user_2").unsafeRunSync().toSet shouldEqual Set(
          GroupMember("user_1"),
          GroupMember("user_2")
        )
        groupDao.findMembersByUserId("user_3").unsafeRunSync().toSet shouldEqual Set(
          GroupMember("user_1"),
          GroupMember("user_3")
        )
      }
      "#allGroup" in {
        val group1 = BSONDocument(
          "id"   -> BSONString("group_1"),
          "name" -> BSONString("Group #1"),
          "members" -> BSONArray(
            BSONDocument("id" -> BSONString("user_1")),
            BSONDocument("id" -> BSONString("user_2"))
          )
        )
        val group2 = BSONDocument(
          "id"   -> BSONString("group_2"),
          "name" -> BSONString("Group #2"),
          "members" -> BSONArray(
            BSONDocument("id" -> BSONString("user_1")),
            BSONDocument("id" -> BSONString("user_3"))
          )
        )
        await(groupCollection.insert.one(group1))
        await(groupCollection.insert.one(group2))

        groupDao.allGroup().unsafeRunSync() shouldEqual Seq(
          GroupAdminListData("group_1", "Group #1", Seq(GroupMember("user_1"), GroupMember("user_2"))),
          GroupAdminListData("group_2", "Group #2", Seq(GroupMember("user_1"), GroupMember("user_3")))
        )
      }
      "#isMember" in {
        val group1 = BSONDocument(
          "id"   -> BSONString("group_1"),
          "name" -> BSONString("Group #1"),
          "members" -> BSONArray(
            BSONDocument("id" -> BSONString("user_1")),
            BSONDocument("id" -> BSONString("user_2"))
          )
        )
        val group2 = BSONDocument(
          "id"   -> BSONString("group_2"),
          "name" -> BSONString("Group #2"),
          "members" -> BSONArray(
            BSONDocument("id" -> BSONString("user_1")),
            BSONDocument("id" -> BSONString("user_3"))
          )
        )
        await(groupCollection.insert.one(group1))
        await(groupCollection.insert.one(group2))

        groupDao.isMember("group_1", "user_1").unsafeRunSync() shouldEqual true
        groupDao.isMember("group_1", "user_2").unsafeRunSync() shouldEqual true
        groupDao.isMember("group_1", "user_3").unsafeRunSync() shouldEqual false
        groupDao.isMember("group_2", "user_1").unsafeRunSync() shouldEqual true
        groupDao.isMember("group_2", "user_2").unsafeRunSync() shouldEqual false
        groupDao.isMember("group_2", "user_3").unsafeRunSync() shouldEqual true
        groupDao.isMember("group_3", "user_4").unsafeRunSync() shouldEqual false
      }
      "#createGroup" in {
        val data = CreateGroupData("Group Name", Seq(GroupMember("user 1"), GroupMember("user 2")))
        groupDao.createGroup(data).unsafeRunSync() shouldEqual "1"

        val result = await(
          groupCollection
            .find(BSONDocument(), None)
            .one[BSONDocument]
        )

        result shouldBe a[Some[_]]
        result.get.get("id") shouldEqual Some(BSONString("1"))
        result.get.get("name") shouldEqual Some(BSONString("Group Name"))
        result.get.get("members") shouldBe a[Some[_]]
        val members = result.get
          .get("members")
          .collect { case x: BSONArray => x.values.toList }
          .get
          .collect { case value: BSONDocument => value.get("id") }
          .collect { case Some(x: BSONString) => x.value }
        members shouldEqual Seq("user 1", "user 2")
      }
      "#addMemberToGroup" in {
        val group1 = BSONDocument(
          "id"   -> BSONString("group_1"),
          "name" -> BSONString("Group #1"),
          "members" -> BSONArray(
            BSONDocument("id" -> BSONString("user_1")),
            BSONDocument("id" -> BSONString("user_2"))
          )
        )
        val group2 = BSONDocument(
          "id"   -> BSONString("group_2"),
          "name" -> BSONString("Group #2"),
          "members" -> BSONArray(
            BSONDocument("id" -> BSONString("user_1")),
            BSONDocument("id" -> BSONString("user_3"))
          )
        )
        await(groupCollection.insert.one(group1))
        await(groupCollection.insert.one(group2))

        groupDao.addMemberToGroup("group_2", GroupMember("new_user_id")).unsafeRunSync()

        groupDao.isMember("group_1", "new_user_id").unsafeRunSync() shouldEqual false
        groupDao.isMember("group_2", "new_user_id").unsafeRunSync() shouldEqual true
        groupDao.isMember("group_2", "user_1").unsafeRunSync() shouldEqual true
        groupDao.isMember("group_2", "user_2").unsafeRunSync() shouldEqual false
        groupDao.isMember("group_2", "user_3").unsafeRunSync() shouldEqual true
      }
    }
  }
}
