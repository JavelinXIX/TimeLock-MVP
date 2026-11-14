package jp.timelock

internal object GuardDecision {
 fun blockedPackages(
  activePackages: Set<String>, limits: Map<String, Int>, totals: Map<String, Long>,
  alreadyBlocked: Set<String>, protectedPackages: Set<String>
 ): Set<String> = activePackages.filterTo(mutableSetOf()) { pkg ->
  pkg !in protectedPackages && limits[pkg]?.let { limit ->
   pkg in alreadyBlocked || (totals[pkg] ?: 0L) >= limit * 60000L
  } == true
 }
}
