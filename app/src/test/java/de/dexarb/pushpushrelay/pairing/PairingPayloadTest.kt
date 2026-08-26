package de.dexarb.pushpushrelay.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `rejects invalid versions`() {
        listOf(0, -1).forEach { v ->
            assertEquals("unsupported version $v", (PairingPayloadParser.parse(validJson(v = v)) as PairingParseResult.Invalid).reason)
        }
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

    private fun validV2Json(paymentMethods: String): String =
        """{"v":2,"type":"pushpush-pairing","notify_url":"https://example.com/notify",""" +
            """"heartbeat_url":"https://example.com/heartbeat","secret":"s3cret","payment_methods":$paymentMethods}"""

    @Test
    fun `parses v2 payload with payment methods`() {
        val raw = validV2Json(
            """[{"id":17,"name":"Revolut","category":"Fiat Transmitter",""" +
                """"alias_list":["Revolut UAB","revolut"],"sources":["binance_p2p"],"currencies":["EUR","USD"]}]"""
        )
        val result = PairingPayloadParser.parse(raw)
        assertTrue(result is PairingParseResult.Parsed)
        result as PairingParseResult.Parsed
        assertEquals(2, result.payload.schemaVersion)
        val methods = result.payload.paymentMethods
        assertEquals(1, methods?.size)
        val method = methods!!.first()
        assertEquals(17L, method.id)
        assertEquals("Revolut", method.name)
        assertEquals("Fiat Transmitter", method.category)
        assertEquals(listOf("Revolut UAB", "revolut"), method.aliasList)
        assertEquals(listOf("binance_p2p"), method.sources)
        assertEquals(listOf("EUR", "USD"), method.currencies)
    }

    @Test
    fun `v2 payload with empty payment methods pairs with empty authorization`() {
        val result = PairingPayloadParser.parse(validV2Json("[]"))
        assertTrue(result is PairingParseResult.Parsed)
        assertEquals(emptyList<PaymentMethod>(), (result as PairingParseResult.Parsed).payload.paymentMethods)
    }

    @Test
    fun `v1 payload has no payment methods`() {
        val result = PairingPayloadParser.parse(validJson())
        assertTrue(result is PairingParseResult.Parsed)
        assertEquals(null, (result as PairingParseResult.Parsed).payload.paymentMethods)
    }

    @Test
    fun `ignores unknown fields inside payment methods`() {
        val raw = validV2Json(
            """[{"id":1,"name":"Wise","category":"Fiat Transmitter","alias_list":[],"sources":[],""" +
                """"currencies":[],"new_field":"later"}]"""
        )
        val result = PairingPayloadParser.parse(raw)
        assertTrue(result is PairingParseResult.Parsed)
    }

    @Test
    fun `parses large payment method lists`() {
        val entries = (1..500).joinToString(",") { i ->
            """{"id":$i,"name":"Provider $i","category":"Bank","alias_list":["Alias $i"],"sources":[],"currencies":["EUR"]}"""
        }
        val result = PairingPayloadParser.parse(validV2Json("[$entries]"))
        assertTrue(result is PairingParseResult.Parsed)
        assertEquals(500, (result as PairingParseResult.Parsed).payload.paymentMethods?.size)
    }

    @Test
    fun `rejects v2 payload with wrong type`() {
        val raw = validV2Json("[]").replace("pushpush-pairing", "other-type")
        val result = PairingPayloadParser.parse(raw)
        assertEquals("unexpected type 'other-type'", (result as PairingParseResult.Invalid).reason)
    }

    @Test
    fun `rejects truncated v2 json instead of pairing partially`() {
        val raw = validV2Json("""[{"id":17,"name":"Revolut","""").dropLast(10)
        val result = PairingPayloadParser.parse(raw)
        assertTrue(result is PairingParseResult.Invalid)
    }

    @Test
    fun `redacts secret in payload toString`() {
        val result = PairingPayloadParser.parse(validV2Json("[]"))
        val text = (result as PairingParseResult.Parsed).payload.toString()
        assertFalse(text.contains("s3cret"))
    }
}
