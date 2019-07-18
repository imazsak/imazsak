package hu.ksisu.imazsak.util

import java.util.concurrent.atomic.AtomicLong

class DateTimeUtilCounterImpl extends DateTimeUtil {
  val counter                             = new AtomicLong(0)
  override def getCurrentTimeMillis: Long = counter.incrementAndGet()
}
