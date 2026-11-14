package jp.timelock

import org.junit.Assert.*
import org.junit.Test

class GuardDecisionTest {
 @Test fun overLimitUsageBlocksWithoutAccessibilityForegroundEvent() {
  val usage = UsageAccumulator(0)
  usage.resume("x", "Main", 1000)
  val blocked = GuardDecision.blockedPackages(usage.activePackages(), mapOf("x" to 60),
   usage.result(1000 + 62 * 60000L), emptySet(), setOf("home"))
  assertEquals(setOf("x"), blocked)
 }
 @Test fun keyboardAndSystemUiDoNotReplaceTheActiveApp() {
  val usage = UsageAccumulator(0)
  usage.resume("x", "Main", 0)
  usage.resume("keyboard", "Ime", 1000)
  usage.resume("systemui", "Notification", 2000)
  assertEquals(setOf("x"), GuardDecision.blockedPackages(usage.activePackages(),
   mapOf("x" to 1), usage.result(61000), emptySet(), emptySet()))
 }
 @Test fun pausedAppIsNotBlockedWhileAnotherAppIsOpen() {
  val usage = UsageAccumulator(0)
  usage.resume("x", "Main", 0)
  usage.pause("x", "Main", 61000)
  usage.resume("other", "Main", 61000)
  assertTrue(GuardDecision.blockedPackages(usage.activePackages(), mapOf("x" to 1),
   usage.result(62000), setOf("x"), emptySet()).isEmpty())
 }
 @Test fun protectsHomeSettingsAndSelfEvenWithStaleRules() {
  assertTrue(GuardDecision.blockedPackages(setOf("home", "settings", "self"),
   mapOf("home" to 1,"settings" to 1,"self" to 1), emptyMap(),
   setOf("home", "settings", "self"), setOf("home", "settings", "self")).isEmpty())
 }
 @Test fun persistedBlockAndExactThresholdAreEnforced() {
  assertEquals(setOf("x"), GuardDecision.blockedPackages(setOf("x"), mapOf("x" to 1), mapOf("x" to 60000L), emptySet(), emptySet()))
  assertEquals(setOf("x"), GuardDecision.blockedPackages(setOf("x"), mapOf("x" to 1), emptyMap(), setOf("x"), emptySet()))
 }
 @Test fun screenOffClearsActiveApps() {
  val usage = UsageAccumulator(0)
  usage.resume("x", "Main", 0)
  usage.stopAll(61000)
  assertTrue(usage.activePackages().isEmpty())
 }
}
