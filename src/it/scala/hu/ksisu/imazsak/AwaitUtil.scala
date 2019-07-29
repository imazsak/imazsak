package hu.ksisu.imazsak

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

trait AwaitUtil {
  implicit val timeout = 10.seconds

  def await[T](f: Future[T]): T = Await.result(f, timeout)
}
