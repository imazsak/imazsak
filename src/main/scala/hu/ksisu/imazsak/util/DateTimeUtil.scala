package hu.ksisu.imazsak.util

trait DateTimeUtil {
  def getCurrentTimeMillis: Long
}

class DateTimeUtilImpl extends DateTimeUtil {
  override def getCurrentTimeMillis: Long = System.currentTimeMillis()
}
