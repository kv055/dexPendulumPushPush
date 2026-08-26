package de.dexarb.pushpushrelay.relay

import de.dexarb.pushpushrelay.pairing.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentMethodMatcherTest {

    private val methods = listOf(
        PaymentMethod(
            id = 17,
            name = "Revolut",
            category = "Fiat Transmitter",
            aliasList = listOf("Revolut UAB", "revolut ltd"),
        ),
        PaymentMethod(id = 3, name = "Deutsche Bank", category = "Bank"),
    )

    @Test
    fun `matches provider name case-insensitively in text`() {
        val matcher = PaymentMethodMatcher(methods)
        assertEquals(methods[0], matcher.match("Revolut", null, null))
        assertEquals(methods[0], matcher.match(null, "Payment received", "You got 50 EUR from REVOLUT"))
    }

    @Test
    fun `matches alias entries`() {
        val matcher = PaymentMethodMatcher(methods)
        assertEquals(methods[0], matcher.match(null, null, "Transfer via Revolut UAB"))
    }

    @Test
    fun `matches app label as sender hint`() {
        val matcher = PaymentMethodMatcher(methods)
        assertEquals(methods[1], matcher.match("deutsche bank", null, "New booking"))
    }

    @Test
    fun `returns null when nothing matches`() {
        val matcher = PaymentMethodMatcher(methods)
        assertNull(matcher.match(null, null, "N26: card payment"))
        assertNull(matcher.match(null, null, null))
        assertNull(matcher.match("", "", ""))
    }

    @Test
    fun `empty method list never matches`() {
        assertNull(PaymentMethodMatcher(emptyList()).match("Revolut", "x", "y"))
    }

    @Test
    fun `blank aliases are ignored`() {
        val withBlankAlias = listOf(PaymentMethod(id = 1, name = "Wise", aliasList = listOf(" ", "")))
        assertNull(PaymentMethodMatcher(withBlankAlias).match(null, null, "transfer"))
    }

    @Test
    fun `matching is substring based across combined fields`() {
        val matcher = PaymentMethodMatcher(methods)
        assertEquals(
            methods[1],
            matcher.match(null, "Deutsche Bank", null) ?: matcher.match(null, null, "your deutsche bank account"),
        )
    }
}
