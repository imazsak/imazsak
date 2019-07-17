package hu.ksisu.imazsak.core

import akka.NotUsed
import akka.http.scaladsl.model.{ContentType, Uri}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import hu.ksisu.imazsak.Initable

trait FileStoreService[F[_]] extends Initable[F] {
  def store(bucket: String, fileName: String, contentType: ContentType, file: Source[ByteString, NotUsed]): F[Uri]
}
