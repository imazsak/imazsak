package hu.ksisu.imazsak.stat

trait StatDao[F[_]] {
  def incrementStat(key: String, dateKey: String, counter: Long): F[Unit]
}
