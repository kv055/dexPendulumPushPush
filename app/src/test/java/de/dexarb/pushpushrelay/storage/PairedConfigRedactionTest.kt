package de.dexarb.pushpushrelay.storage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PairedConfigRedactionTest {

    @Test
    fun `toString redacts secret`() {
        val config = PairedConfig(
            notifyUrl = "https://example.test/notify",
            heartbeatUrl = "https://example.test/heartbeat",
            secret = "super-secret-value",
            deviceId = "payment-phone-0123abcd",
        )
        val text = config.toString()
        assertTrue(text.contains("REDACTED"))
        assertFalse(text.contains("super-secret-value"))
    }

    @Test
    fun `deviceId format is prefix plus 8 hex chars`() {
        val id = DeviceIdGenerator.generate()
        assertTrue(id.matches(Regex("^payment-phone-[0-9a-f]{8}$")))
    }
}
