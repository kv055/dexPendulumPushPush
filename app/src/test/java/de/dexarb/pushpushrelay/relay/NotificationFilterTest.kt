package de.dexarb.pushpushrelay.relay

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationFilterTest {

    private val filter = NotificationFilter(NotificationFilter.DefaultPackages)

    private fun assertCases(filter: NotificationFilter, cases: List<Pair<String, Boolean>>) =
        cases.forEach { (pkg, expected) ->
            assertEquals("package=$pkg", expected, filter.shouldRelay(pkg))
        }

    @Test
    fun `default allowlist relays known banking apps`() {
        assertCases(
            filter,
            listOf(
                "com.paypal.android.p2pmobile" to true,
                "com.revolut.revolut" to true,
                "com.transferwise.android" to true,
                "com.n26.android" to true,
                "com.sparkasse.zhk" to true,
                "com.dkbcodefactory.banking" to true,
                "com.commerzbanking.app0" to true,
                "com.db.pwcc" to true,
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
    fun `unknown packages are not relayed`() {
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
                "com.revolut.revolut" to false,
                "" to false,
            ),
        )
    }

    @Test
    fun `custom allowlist is honored`() {
        val custom = NotificationFilter(setOf("my.custom.app"))
        assertEquals(true, custom.shouldRelay("my.custom.app"))
        assertEquals(false, custom.shouldRelay("com.revolut.revolut"))
    }
}
