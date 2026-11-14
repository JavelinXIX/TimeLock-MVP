package jp.timelock

/** Same-process heartbeat: enabling the accessibility permission does not prove the service runs. */
internal object GuardStatus {
 @Volatile var lastTickElapsed: Long = 0L
 @Volatile var error: Boolean = false
 fun isAlive(now: Long) = lastTickElapsed > 0L && now - lastTickElapsed in 0..5000
}
