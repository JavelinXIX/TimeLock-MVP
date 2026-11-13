package jp.timelock

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

class Rules(context: Context) {
 private val prefs = context.getSharedPreferences("rules", Context.MODE_PRIVATE)
 private val policy = DailyPolicy(
  read = { key -> prefs.getString(key, null)?.let { decode(it) } },
  write = { key, values -> check(prefs.edit().putString(key, JSONObject(values).toString()).commit()) },
  date = { LocalDate.now().toString() }
 )
 @Synchronized fun today() = policy.today()
 fun blocked(pkg: String): Boolean = prefs.getStringSet("blocked:${LocalDate.now()}", emptySet())!!.contains(pkg)
 @Synchronized fun markBlocked(pkg: String) {
  val key = "blocked:${LocalDate.now()}"
  check(prefs.edit().putStringSet(key, prefs.getStringSet(key, emptySet())!!.toSet() + pkg).commit())
 }
 fun plan() = policy.plan()
 @Synchronized fun schedule(pkg: String, minutes: Int) = policy.schedule(pkg, minutes)
 private fun decode(raw: String): Map<String, Int> {
  val obj = JSONObject(raw)
  return obj.keys().asSequence().associateWith { obj.getInt(it) }
 }
}
