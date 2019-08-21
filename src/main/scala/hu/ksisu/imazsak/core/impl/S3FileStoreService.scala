package hu.ksisu.imazsak.core.impl

import akka.NotUsed
import akka.http.scaladsl.model.{ContentType, Uri}
import akka.stream.Materializer
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.Source
import akka.util.ByteString
import cats.effect.{ContextShift, IO}
import hu.ksisu.imazsak.core.FileStoreService

import scala.concurrent.ExecutionContext

class S3FileStoreService(implicit ec: ExecutionContext, mat: Materializer, cs: ContextShift[IO])
    extends FileStoreService[IO] {

  override def init: IO[Unit] = IO.pure(())

  override def store(
      bucket: String,
      fileName: String,
      contentType: ContentType,
      file: Source[ByteString, NotUsed]
  ): IO[Uri] = {
    val s3Sink = S3.multipartUpload(bucket, fileName, contentType)
    IO.fromFuture(IO(file.runWith(s3Sink).map(_.location)))
  }

}
