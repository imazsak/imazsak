package hu.ksisu.imazsak.core.impl

import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl.{AmqpSink, AmqpSource}
import akka.stream.scaladsl.{Keep, Sink, Source, SourceQueueWithComplete}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.ByteString
import akka.{Done, NotUsed}
import cats.Applicative
import hu.ksisu.imazsak.core.AmqpService
import hu.ksisu.imazsak.core.AmqpService.{AmqpConfig, AmqpQueueConfig, AmqpSenderWrapper}

import scala.concurrent.Future

class AmqpServiceImpl[F[_]: Applicative](implicit config: AmqpConfig, mat: Materializer) extends AmqpService[F] {
  import cats.syntax.applicative._

  protected lazy val connection: AmqpConnectionProvider = {
    AmqpCachedConnectionProvider(
      AmqpUriConnectionProvider(config.uri)
    )
  }

  override def init: F[Unit] = {
    connection
    ().pure[F]
  }

  override def createSenderWrapper(queueConfig: AmqpQueueConfig): AmqpSenderWrapper = {

    val amqpSink: Sink[ByteString, Future[Done]] = {
      AmqpSink.simple(
        convertToWriteSettings(connection, queueConfig)
      )
    }

    val queue: SourceQueueWithComplete[ByteString] = {
      Source
        .queue[ByteString](queueConfig.bufferSize, OverflowStrategy.fail)
        .toMat(amqpSink)(Keep.left)
        .run()
    }

    new AmqpSenderWrapper(queue)
  }

  override def createQueueSource(queueConfig: AmqpQueueConfig): Source[ReadResult, NotUsed] = {
    AmqpSource.atMostOnceSource(
      NamedQueueSourceSettings(connection, queueConfig.routingKey.get).withAckRequired(false),
      bufferSize = 10
    )
  }

  private def convertToWriteSettings(connectionProvider: AmqpConnectionProvider, queueConfig: AmqpQueueConfig) = {
    val ws0 = AmqpWriteSettings(connectionProvider)
    val ws1 = queueConfig.exchange.map(ws0.withExchange).getOrElse(ws0)
    queueConfig.routingKey.map(ws1.withRoutingKey).getOrElse(ws1)
  }
}
