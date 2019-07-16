package hu.ksisu.imazsak.util

import java.util.concurrent.atomic.AtomicLong

class IdGeneratorCounterImpl extends IdGenerator {
  val counter                     = new AtomicLong(0)
  override def generate(): String = counter.incrementAndGet().toString
  def reset(): Unit               = counter.set(0)
}
