package jp.timelock

/** Storage is synchronous so the current day's snapshot is durable before editing plans. */
internal class DailyPolicy(
 private val read: (String) -> Map<String, Int>?,
 private val write: (String, Map<String, Int>) -> Unit,
 private val date: () -> String
) {
 fun today(): Map<String, Int> {
  val key = "day:${date()}"
  return read(key) ?: (read("plan") ?: emptyMap()).toMap().also { write(key, it) }
 }
 fun plan() = read("plan") ?: emptyMap()
 fun schedule(pkg: String, minutes: Int) {
  require(minutes in 0..1440)
  today()
  val next = plan().toMutableMap()
  if (minutes == 0) next.remove(pkg) else next[pkg] = minutes
  write("plan", next.toMap())
 }
}
