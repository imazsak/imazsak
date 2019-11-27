package hu.ksisu.imazsak.core.dao

import cats.data.OptionT
import cats.effect.{ContextShift, IO}
import hu.ksisu.imazsak.core.dao.BsonHelper._
import hu.ksisu.imazsak.util.IdGenerator
import reactivemongo.api.Cursor
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter}

import scala.concurrent.ExecutionContext

object MongoQueryHelper {
  def insert[T](data: T)(
      implicit collectionF: IO[BSONCollection],
      writer: BSONDocumentWriter[T],
      idGenerator: IdGenerator,
      ec: ExecutionContext,
      cs: ContextShift[IO]
  ): IO[String] = {
    val model = data.toBsonWithNewId
    for {
      collection <- collectionF
      _          <- IO.fromFuture(IO(collection.insert(false).one(model)))
    } yield model.getId
  }

  def list[T](
      selector: BSONDocument,
      projector: Option[BSONDocument] = None,
      sort: Option[BSONDocument] = None,
      limit: Option[Int] = None
  )(
      implicit collectionF: IO[BSONCollection],
      reader: BSONDocumentReader[T],
      ec: ExecutionContext,
      cs: ContextShift[IO]
  ): IO[Seq[T]] = {
    def createQuery(collection: BSONCollection): IO[Seq[T]] = {
      val find   = collection.find(selector, projector)
      val sorted = sort.fold(find)(find.sort)
      limit match {
        case Some(0) => IO.pure(Seq.empty[T])
        case l =>
          val query = sorted
            .cursor[T]()
            .collect[Seq](l.getOrElse(-1), Cursor.FailOnError[Seq[T]]())
          IO.fromFuture(IO(query))
      }
    }

    for {
      collection <- collectionF
      result     <- createQuery(collection)
    } yield result
  }

  def sortedList[T](
      selector: BSONDocument,
      sorter: BSONDocument,
      projector: Option[BSONDocument] = None,
      limit: Option[Int] = None
  )(
      implicit collectionF: IO[BSONCollection],
      reader: BSONDocumentReader[T],
      ec: ExecutionContext,
      cs: ContextShift[IO]
  ): IO[Seq[T]] = {
    for {
      collection <- collectionF
      groups <- IO.fromFuture(
        IO(
          collection
            .find(selector, projector)
            .sort(sorter)
            .cursor[T]()
            .collect[Seq](limit.getOrElse(-1), Cursor.FailOnError[Seq[T]]())
        )
      )
    } yield groups
  }

  def findOne[T](selector: BSONDocument, projector: Option[BSONDocument] = None)(
      implicit collectionF: IO[BSONCollection],
      reader: BSONDocumentReader[T],
      ec: ExecutionContext,
      cs: ContextShift[IO]
  ): OptionT[IO, T] = {
    OptionT(for {
      collection <- collectionF
      result     <- IO.fromFuture(IO(collection.find(selector, projector).one[T]))
    } yield result)
  }

  def updateOne(selector: BSONDocument, modifier: BSONDocument)(
      implicit collectionF: IO[BSONCollection],
      ec: ExecutionContext,
      cs: ContextShift[IO]
  ): IO[Unit] = {
    for {
      collection <- collectionF
      _          <- IO.fromFuture(IO(collection.update.one(selector, modifier, upsert = false, multi = false)))
    } yield ()
  }

  def updateMultiple(selector: BSONDocument, modifier: BSONDocument)(
      implicit collectionF: IO[BSONCollection],
      ec: ExecutionContext,
      cs: ContextShift[IO]
  ): IO[Unit] = {
    for {
      collection <- collectionF
      _          <- IO.fromFuture(IO(collection.update.one(selector, modifier, upsert = false, multi = true)))
    } yield ()
  }

  def deleteOne(selector: BSONDocument)(
      implicit collectionF: IO[BSONCollection],
      ec: ExecutionContext,
      cs: ContextShift[IO]
  ): IO[Int] = {
    deleteMultipleWithLimit(selector, Some(1))
  }

  def deleteMultiple(selector: BSONDocument)(
      implicit collectionF: IO[BSONCollection],
      ec: ExecutionContext,
      cs: ContextShift[IO]
  ): IO[Int] = {
    deleteMultipleWithLimit(selector, None)
  }

  private def deleteMultipleWithLimit(selector: BSONDocument, limit: Option[Int])(
      implicit collectionF: IO[BSONCollection],
      ec: ExecutionContext,
      cs: ContextShift[IO]
  ): IO[Int] = {
    for {
      collection <- collectionF
      result     <- IO.fromFuture(IO(collection.delete.one(selector, limit = limit)))
    } yield result.n
  }
}
