package de.dexarb.pushpushrelay.storage

import java.util.concurrent.ConcurrentHashMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class InMemoryPairedConfigStore : PairedConfigStore {
    private var stored: PairedConfig? = null

    override fun load(): PairedConfig? = stored

    override fun save(config: PairedConfig) {
        stored = config
    }

    override fun clear() {
        stored = null
    }
}

class PairedConfigTest {

    @Test
    fun `round-trips config through fake store`() {
        val store = InMemoryPairedConfigStore()
        assertNull(store.load())

        val config = PairedConfig(
            notifyUrl = "https://example.com/notify",
            heartbeatUrl = "https://example.com/heartbeat",
            secret = "s3cret",
            deviceId = DeviceIdGenerator.generate(),
        )
        store.save(config)

        val loaded = store.load()
        assertEquals(config, loaded)
    }

    @Test
    fun `save overwrites and clear empties store`() {
        val store = InMemoryPairedConfigStore()
        store.save(PairedConfig("a", "b", "c", "d"))
        store.save(PairedConfig("e", "f", "g", "h"))
        assertEquals(PairedConfig("e", "f", "g", "h"), store.load())

        store.clear()
        assertNull(store.load())
    }

    @Test
    fun `generated device id matches format`() {
        repeat(100) {
            assertTrue(
                DeviceIdGenerator.generate()
                    .matches(Regex("^payment-phone-[0-9a-f]{8}$"))
            )
        }
    }

    @Test
    fun `generated device ids are unique`() {
        val ids = (1..1000).map { DeviceIdGenerator.generate() }.toSet()
        assertEquals(1000, ids.size)
    }
}
