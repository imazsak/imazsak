package hu.ksisu.imazsak.util

import java.util.UUID

trait IdGenerator {
  def generate(): String
}

class IdGeneratorImpl extends IdGenerator {
  override def generate(): String = UUID.randomUUID().toString
}
