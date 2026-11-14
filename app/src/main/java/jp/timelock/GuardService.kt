package jp.timelock

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalDate

class GuardService : AccessibilityService() {
 private val handler = Handler(Looper.getMainLooper())
 private var blockedDay: LocalDate? = null
 private var panel: LinearLayout? = null
 private var lastAttemptElapsed = 0L
 private val tick = object : Runnable {
  override fun run() {
   try { checkSafely() } finally { handler.postDelayed(this, 1000) }
  }
 }
 override fun onServiceConnected() {
  handler.removeCallbacks(tick)
  GuardStatus.error = false
  handler.post(tick)
 }
 override fun onAccessibilityEvent(event: AccessibilityEvent?) {
  // Events only accelerate a check. IME/System UI events must never replace the monitored app.
  if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
   SystemClock.elapsedRealtime() - lastAttemptElapsed >= 500L) checkSafely()
 }
 private fun checkSafely() {
  lastAttemptElapsed = SystemClock.elapsedRealtime()
  GuardStatus.lastTickElapsed = lastAttemptElapsed
  try {
   checkLimit()
   GuardStatus.error = false
  } catch (e: Exception) {
   // An overlay or usage-access failure must not permanently stop the repeating monitor.
   GuardStatus.error = true
   Log.e("TimeLock", "Block monitor check failed", e)
  }
 }
 private fun checkLimit() {
  if (panel != null && blockedDay != LocalDate.now()) dismissPanel()
  if (!getSystemService(PowerManager::class.java).isInteractive ||
   getSystemService(KeyguardManager::class.java).isKeyguardLocked || !Usage.allowed(this)) return

  val rules = Rules(this)
  val limits = rules.today()
  if (limits.isEmpty()) return
  // Query even when there is no accessibility event, e.g. service connected while X was open.
  val snapshot = Usage.snapshot(this)
  val home = packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName
  val protected = setOfNotNull(packageName, "com.android.settings", home)
  val blocked = GuardDecision.blockedPackages(
   snapshot.activePackages, limits, snapshot.totals,
   limits.keys.filterTo(mutableSetOf()) { rules.blocked(it) }, protected
  )
  if (blocked.isEmpty()) return
  blocked.forEach { if (!rules.blocked(it)) rules.markBlocked(it) }
  performGlobalAction(GLOBAL_ACTION_HOME)
  if (panel != null) return

  val layout = LinearLayout(this).apply {
   orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
   setPadding(48,48,48,48); setBackgroundColor(0xFF102532.toInt())
   addView(TextView(this@GuardService).apply {
    text = "本日の利用時間は終了しました\n明日0時にリセットされます"
    textSize = 24f; setTextColor(-1); gravity = Gravity.CENTER
   })
   addView(Button(this@GuardService).apply {
    text = "ホームへ戻る"
    setOnClickListener { performGlobalAction(GLOBAL_ACTION_HOME); dismissPanel() }
   })
  }
  getSystemService(WindowManager::class.java).addView(layout,
   WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.OPAQUE))
  // Publish only after addView succeeds, so a failed overlay is retried on the next check.
  blockedDay = LocalDate.now()
  panel = layout
 }
 private fun dismissPanel() {
  panel?.let { getSystemService(WindowManager::class.java).removeView(it) }
  panel = null
 }
 override fun onInterrupt() {}
 override fun onDestroy() {
  handler.removeCallbacksAndMessages(null)
  GuardStatus.lastTickElapsed = 0L
  try { dismissPanel() } finally { super.onDestroy() }
 }
}
