package jp.timelock
import org.junit.Assert.*
import org.junit.Test
class DailyPolicyTest {
 private val store = mutableMapOf<String, Map<String,Int>>()
 private var date = "2026-11-13"
 private fun policy() = DailyPolicy({ store[it] }, { key, value -> store[key] = value.toMap() }, { date })
 @Test fun firstSetupOnlyAppliesTomorrowAndSurvivesRecreation() {
  policy().schedule("a", 10)
  assertTrue(policy().today().isEmpty())
  date = "2026-11-14"
  assertEquals(10, policy().today()["a"])
 }
 @Test fun cannotExtendOrRemoveTodaysLimit() {
  store["plan"] = mapOf("a" to 10)
  assertEquals(10, policy().today()["a"])
  policy().schedule("a", 100)
  assertEquals(10, policy().today()["a"])
  policy().schedule("a", 0)
  assertEquals(10, policy().today()["a"])
  date = "2026-11-14"
  assertTrue(policy().today().isEmpty())
 }
 @Test(expected = IllegalArgumentException::class) fun rejectsInvalidLimit() { policy().schedule("a", 1441) }
}
