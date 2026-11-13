package jp.timelock
import org.junit.Assert.assertEquals
import org.junit.Test
class UsageAccumulatorTest {
 @Test fun clipsMidnightAndIncludesOpenSession() {
  val usage = UsageAccumulator(1000)
  usage.resume("a", "Main", 900)
  assertEquals(500L, usage.result(1500)["a"])
 }
 @Test fun overlappingActivitiesAreNotDoubleCounted() {
  val usage = UsageAccumulator(0)
  usage.resume("a", "Main", 100)
  usage.resume("a", "Other", 200)
  usage.pause("a", "Main", 300)
  usage.pause("a", "Other", 400)
  usage.pause("a", "Other", 500)
  assertEquals(300L, usage.result(600)["a"])
 }
 @Test fun screenOffStopsSessionAndResumeStartsAgain() {
  val usage = UsageAccumulator(0)
  usage.resume("a", "Main", 100)
  usage.stopAll(200)
  usage.resume("a", "Main", 500)
  assertEquals(200L, usage.result(600)["a"])
 }
}
