package hu.ksisu.imazsak

import akka.actor.ActorSystem
import hu.ksisu.imazsak.util.LoggerUtil.LogContext
import io.opentracing.noop.NoopTracerFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait TestBase extends WordSpecLike with Matchers with MockitoSugar with BeforeAndAfterAll {

  implicit lazy val dummyLogContext = {
    val tracer = NoopTracerFactory.create()
    val span   = tracer.buildSpan("test").start()
    new LogContext(tracer, span)
  }

  val timeout = 1.second

  def await[T](f: Future[T]): T = Await.result(f, timeout)

  def withActorSystem[R](block: ActorSystem => R): R = {
    val as = ActorSystem()
    try {
      block(as)
    } finally {
      await(as.terminate())
    }
  }
}
