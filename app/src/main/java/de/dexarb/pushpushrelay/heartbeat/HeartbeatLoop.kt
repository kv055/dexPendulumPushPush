package de.dexarb.pushpushrelay.heartbeat

import kotlinx.coroutines.delay

class HeartbeatLoop(
    private val intervalMillis: Long = DEFAULT_INTERVAL_MILLIS,
    private val sleep: suspend (Long) -> Unit = { delay(it) },
) {
    suspend fun run(action: () -> Boolean, onResult: (Boolean) -> Unit = {}) {
        while (true) {
            onResult(runCatching { action() }.getOrDefault(false))
            sleep(intervalMillis)
        }
    }

    companion object {
        const val DEFAULT_INTERVAL_MILLIS: Long = 5 * 60 * 1000L
    }
}
