package de.dexarb.pushpushrelay.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingPayloadTest {

    private fun validJson(
        v: Int = 1,
        type: String = "pushpush-pairing",
        notifyUrl: String = "https://example.com/notify",
        heartbeatUrl: String = "https://example.com/heartbeat",
        secret: String = "s3cret",
    ): String =
        """{"v":$v,"type":"$type","notify_url":"$notifyUrl","heartbeat_url":"$heartbeatUrl","secret":"$secret"}"""

    @Test
    fun `parses valid payload`() {
        val result = PairingPayloadParser.parse(validJson())
        assertTrue(result is PairingParseResult.Parsed)
        result as PairingParseResult.Parsed
        assertEquals("https://example.com/notify", result.payload.notifyUrl)
        assertEquals("https://example.com/heartbeat", result.payload.heartbeatUrl)
        assertEquals("s3cret", result.payload.secret)
    }

    @Test
    fun `tolerates unknown extra keys`() {
        val raw = validJson().replace("}", ""","extra":"ignored","n":42}""")
        val result = PairingPayloadParser.parse(raw)
        assertTrue(result is PairingParseResult.Parsed)
    }

    @Test
    fun `accepts http urls for dev`() {
        val result = PairingPayloadParser.parse(
            validJson(notifyUrl = "http://10.0.2.2:8080/notify")
        )
        assertTrue(result is PairingParseResult.Parsed)
    }

    @Test
    fun `rejects wrong version`() {
        val result = PairingPayloadParser.parse(validJson(v = 2))
        assertEquals("unsupported version 2", (result as PairingParseResult.Invalid).reason)
    }

    @Test
    fun `rejects wrong type`() {
        val result = PairingPayloadParser.parse(validJson(type = "other-type"))
        assertEquals("unexpected type 'other-type'", (result as PairingParseResult.Invalid).reason)
    }

    @Test
    fun `rejects malformed json`() {
        val result = PairingPayloadParser.parse("{not json")
        assertTrue(result is PairingParseResult.Invalid)
    }

    @Test
    fun `rejects missing fields`() {
        listOf(
            """{"type":"pushpush-pairing","notify_url":"https://e.com/n","heartbeat_url":"https://e.com/h","secret":"x"}""",
            """{"v":1,"notify_url":"https://e.com/n","heartbeat_url":"https://e.com/h","secret":"x"}""",
            """{"v":1,"type":"pushpush-pairing","heartbeat_url":"https://e.com/h","secret":"x"}""",
            """{"v":1,"type":"pushpush-pairing","notify_url":"https://e.com/n","secret":"x"}""",
            """{"v":1,"type":"pushpush-pairing","notify_url":"https://e.com/n","heartbeat_url":"https://e.com/h"}""",
        ).forEach { raw ->
            assertTrue("raw=$raw", PairingPayloadParser.parse(raw) is PairingParseResult.Invalid)
        }
    }

    @Test
    fun `rejects bad urls`() {
        listOf(
            "",
            "   ",
            "ftp://example.com/notify",
            "https://",
            "not a url",
        ).forEach { url ->
            val result = PairingPayloadParser.parse(validJson(notifyUrl = url))
            assertTrue("url='$url'", result is PairingParseResult.Invalid)
        }
        val result = PairingPayloadParser.parse(validJson(heartbeatUrl = "gopher://x"))
        assertTrue(result is PairingParseResult.Invalid)
    }

    @Test
    fun `rejects blank secret`() {
        val result = PairingPayloadParser.parse(validJson(secret = "  "))
        assertEquals("blank secret", (result as PairingParseResult.Invalid).reason)
    }

    @Test
    fun `rejects too long secret`() {
        val result = PairingPayloadParser.parse(validJson(secret = "a".repeat(257)))
        assertTrue(result is PairingParseResult.Invalid)
    }

    @Test
    fun `accepts max length secret`() {
        val result = PairingPayloadParser.parse(validJson(secret = "a".repeat(256)))
        assertTrue(result is PairingParseResult.Parsed)
    }
}
