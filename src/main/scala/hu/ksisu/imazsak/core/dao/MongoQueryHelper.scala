package hu.ksisu.imazsak.core.dao

import cats.data.OptionT
import hu.ksisu.imazsak.core.dao.BsonHelper._
import hu.ksisu.imazsak.util.IdGenerator
import reactivemongo.api.Cursor
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONDocumentReader, BSONWriter}

import scala.concurrent.{ExecutionContext, Future}

object MongoQueryHelper {
  def insert[T, B <: BSONDocument](data: T)(
      implicit collectionF: Future[BSONCollection],
      writer: BSONWriter[T, B],
      idGenerator: IdGenerator,
      ec: ExecutionContext
  ): Future[String] = {
    val model = data.toBsonWithNewId
    for {
      collection <- collectionF
      _          <- collection.insert(false).one(model)
    } yield model.getId
  }

  def list[T](selector: BSONDocument, projector: Option[BSONDocument] = None)(
      implicit collectionF: Future[BSONCollection],
      reader: BSONDocumentReader[T],
      ec: ExecutionContext
  ): Future[Seq[T]] = {
    for {
      collection <- collectionF
      groups <- collection
        .find(selector, projector)
        .cursor[T]()
        .collect[Seq](-1, Cursor.FailOnError[Seq[T]]())
    } yield groups
  }

  def findOne[T](selector: BSONDocument, projector: Option[BSONDocument] = None)(
      implicit collectionF: Future[BSONCollection],
      reader: BSONDocumentReader[T],
      ec: ExecutionContext
  ): OptionT[Future, T] = {
    OptionT(for {
      collection <- collectionF
      userData   <- collection.find(selector, projector).one[T]
    } yield userData)
  }

  def updateOne(selector: BSONDocument, modifier: BSONDocument)(
      implicit collectionF: Future[BSONCollection],
      ec: ExecutionContext
  ): Future[Unit] = {
    for {
      collection <- collectionF
      _          <- collection.update.one(selector, modifier, upsert = false, multi = false)
    } yield ()
  }

  def deleteOne(selector: BSONDocument)(
      implicit collectionF: Future[BSONCollection],
      ec: ExecutionContext
  ): Future[Unit] = {
    for {
      collection <- collectionF
      _          <- collection.delete.one(selector, limit = Some(1))
    } yield ()
  }
}
