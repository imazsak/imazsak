package hu.ksisu.imazsak.core

import akka.NotUsed
import akka.stream.alpakka.amqp.ReadResult
import akka.stream.scaladsl.{Source, SourceQueueWithComplete}
import akka.util.ByteString
import com.typesafe.config.Config
import hu.ksisu.imazsak.Initable
import hu.ksisu.imazsak.core.AmqpService.{AmqpQueueConfig, AmqpSenderWrapper}

import scala.util.Try

trait AmqpService[F[_]] extends Initable[F] {
  def createSenderWrapper(queueConfig: AmqpQueueConfig): AmqpSenderWrapper
  def createQueueSource(queueConfig: AmqpQueueConfig): Source[ReadResult, NotUsed]
}

object AmqpService {
  case class AmqpConfig(uri: String)

  case class AmqpQueueConfig(routingKey: Option[String], exchange: Option[String], bufferSize: Int)

  object AmqpQueueConfig {
    def apply(config: Config): AmqpQueueConfig = {
      AmqpQueueConfig(
        Try(config.getString("routing-key")).toOption,
        Try(config.getString("exchange")).toOption,
        config.getInt("buffer-size")
      )
    }
  }

  class AmqpSenderWrapper(queue: SourceQueueWithComplete[ByteString]) {
    import spray.json._
    def send[A](msg: A)(implicit msgWriter: JsonWriter[A]): Unit = {
      val message = ByteString(msg.toJson.compactPrint.getBytes("UTF8"))
      queue.offer(message)
    }
  }
}
