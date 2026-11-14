package jp.timelock

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId

data class UsageSnapshot(val totals: Map<String, Long>, val activePackages: Set<String>)

object Usage {
 fun allowed(c: Context): Boolean = c.getSystemService(AppOpsManager::class.java).checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName) == AppOpsManager.MODE_ALLOWED
 fun totals(c: Context): Map<String, Long> = snapshot(c).totals
 fun snapshot(c: Context): UsageSnapshot {
  val now = System.currentTimeMillis()
  val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
  val manager = c.getSystemService(UsageStatsManager::class.java)
  // Query a look-back window to capture an activity already resumed at midnight.
  val events = manager.queryEvents(start - 86400000L, now)
  val accumulator = UsageAccumulator(start)
  val event = UsageEvents.Event()
  while (events.hasNextEvent()) {
   events.getNextEvent(event)
   if (event.eventType == UsageEvents.Event.DEVICE_SHUTDOWN || event.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE) {
    accumulator.stopAll(event.timeStamp)
    continue
   }
   val pkg = event.packageName ?: continue
   val id = event.className ?: pkg
   when (event.eventType) {
    UsageEvents.Event.ACTIVITY_RESUMED -> accumulator.resume(pkg, id, event.timeStamp)
    UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> accumulator.pause(pkg, id, event.timeStamp)
   }
  }
  return UsageSnapshot(accumulator.result(now), accumulator.activePackages())
 }
}
