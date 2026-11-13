package jp.timelock

/** Counts the union of resumed activities within each package, clipped to the day. */
internal class UsageAccumulator(private val start: Long) {
 private val active = mutableMapOf<String, MutableSet<String>>()
 private val since = mutableMapOf<String, Long>()
 private val total = mutableMapOf<String, Long>()
 fun resume(pkg: String, id: String, at: Long) {
  val set = active.getOrPut(pkg) { mutableSetOf() }
  if (set.isEmpty()) since[pkg] = at
  set.add(id)
 }
 fun pause(pkg: String, id: String, at: Long) {
  active[pkg]?.let { it.remove(id); if (it.isEmpty()) close(pkg, at) }
 }
 fun stopAll(at: Long) { since.keys.toList().forEach { close(it, at) }; active.clear() }
 private fun close(pkg: String, at: Long) { since.remove(pkg)?.let { total[pkg] = (total[pkg] ?: 0L) + (at - maxOf(it, start)).coerceAtLeast(0L) } }
 fun result(now: Long): Map<String, Long> = total.toMutableMap().apply {
  since.forEach { (pkg, at) -> this[pkg] = (this[pkg] ?: 0L) + (now - maxOf(at, start)).coerceAtLeast(0L) }
 }
}
