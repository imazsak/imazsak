package hu.ksisu.imazsak.core.impl

import akka.NotUsed
import akka.http.scaladsl.model.{ContentType, Uri}
import akka.stream.Materializer
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.Source
import akka.util.ByteString
import hu.ksisu.imazsak.core.FileStoreService

import scala.concurrent.{ExecutionContext, Future}

class S3FileStoreService(implicit ec: ExecutionContext, mat: Materializer) extends FileStoreService[Future] {

  override def init: Future[Unit] = Future.successful({})

  override def store(
      bucket: String,
      fileName: String,
      contentType: ContentType,
      file: Source[ByteString, NotUsed]
  ): Future[Uri] = {
    val s3Sink = S3.multipartUpload(bucket, fileName, contentType)
    file.runWith(s3Sink).map(_.location)
  }

}
