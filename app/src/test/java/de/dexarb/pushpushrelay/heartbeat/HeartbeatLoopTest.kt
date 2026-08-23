package de.dexarb.pushpushrelay.heartbeat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HeartbeatLoopTest {

    @Test
    fun `executes immediately then once per interval`() = runTest {
        val executed = mutableListOf<Long>()
        val loop = HeartbeatLoop(intervalMillis = 1000L)

        val job = launch {
            loop.run(
                action = { true },
                onResult = { executed.add(currentTime) },
            )
        }
        advanceTimeBy(3500)
        runCurrent()
        job.cancel()

        assertEquals(listOf(0L, 1000L, 2000L, 3000L), executed)
    }

    @Test
    fun `reports false when action throws`() = runTest {
        var lastResult: Boolean? = null
        val loop = HeartbeatLoop(intervalMillis = 10L)
        val job = launch {
            loop.run(action = { error("boom") }, onResult = { lastResult = it })
        }
        advanceTimeBy(20)
        runCurrent()
        job.cancel()
        assertEquals(false, lastResult)
    }
}
