package de.dexarb.pushpushrelay.relay

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationFilterTest {

    private val apps = setOf(
        "com.paypal.android.p2pmobile",
        "com.revolut.revolut",
        "com.sparkasse.zhk",
    )
    private val filter = NotificationFilter(apps)

    private fun assertCases(filter: NotificationFilter, cases: List<Pair<String, Boolean>>) =
        cases.forEach { (pkg, expected) ->
            assertEquals("package=$pkg", expected, filter.shouldRelay(pkg))
        }

    @Test
    fun `selected apps are relayed`() {
        assertCases(
            filter,
            listOf(
                "com.paypal.android.p2pmobile" to true,
                "com.sparkasse.zhk" to true,
            ),
        )
    }

    @Test
    fun `matching is case-insensitive`() {
        assertCases(
            filter,
            listOf(
                "COM.SPARKASSE.ZHK" to true,
                "Com.Revolut.Revolut" to true,
            ),
        )
    }

    @Test
    fun `unselected and unknown packages are not relayed`() {
        assertCases(
            filter,
            listOf(
                "com.whatsapp" to false,
                "com.android.chrome" to false,
                "com.sparkasse.zhk.fake" to false,
                "" to false,
            ),
        )
    }

    @Test
    fun `empty allowlist relays nothing`() {
        val emptyFilter = NotificationFilter(emptySet())
        assertCases(
            emptyFilter,
            listOf(
                "com.paypal.android.p2pmobile" to false,
                "" to false,
            ),
        )
    }
}
