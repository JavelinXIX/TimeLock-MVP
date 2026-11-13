package jp.timelock

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class GuardService : AccessibilityService() {
 private val handler = Handler(Looper.getMainLooper())
 private var foreground: String? = null
 private var blockedDay: java.time.LocalDate? = null
 private var panel: LinearLayout? = null
 private val tick = object : Runnable {
  override fun run() { checkLimit(); handler.postDelayed(this, 1000) }
 }
 override fun onServiceConnected() { handler.post(tick) }
 override fun onAccessibilityEvent(event: AccessibilityEvent?) {
  if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
   foreground = event.packageName?.toString()
   checkLimit()
  }
 }
 private fun checkLimit() {
  if (panel != null && blockedDay != java.time.LocalDate.now()) {
   panel?.let { getSystemService(WindowManager::class.java).removeView(it) }; panel = null
  }
  val pkg = foreground ?: return
  val home = packageManager.resolveActivity(android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName
  if (pkg == packageName || pkg == "com.android.settings" || pkg == home) return
  if (!Usage.allowed(this)) return
  val rules = Rules(this)
  val limit = rules.today()[pkg] ?: return
  if (!rules.blocked(pkg)) {
   if ((Usage.totals(this)[pkg] ?: 0L) < limit * 60000L) return
   rules.markBlocked(pkg)
  }
  performGlobalAction(GLOBAL_ACTION_HOME)
  if (panel != null) return
  val wm = getSystemService(WindowManager::class.java)
  val layout = LinearLayout(this).apply {
   orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(48,48,48,48); setBackgroundColor(0xFF102532.toInt())
   addView(TextView(this@GuardService).apply { text = "本日の利用時間は終了しました\n明日0時にリセットされます"; textSize = 24f; setTextColor(-1); gravity = Gravity.CENTER })
   addView(Button(this@GuardService).apply { text = "ホームへ戻る"; setOnClickListener { performGlobalAction(GLOBAL_ACTION_HOME); panel?.let { wm.removeView(it) }; panel = null } })
  }
  blockedDay = java.time.LocalDate.now()
  panel = layout
  wm.addView(layout, WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.OPAQUE))
 }
 override fun onInterrupt() {}
 override fun onDestroy() { handler.removeCallbacksAndMessages(null); panel?.let { getSystemService(WindowManager::class.java).removeView(it) }; super.onDestroy() }
}
