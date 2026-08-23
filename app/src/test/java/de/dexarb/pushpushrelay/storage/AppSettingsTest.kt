package de.dexarb.pushpushrelay.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import de.dexarb.pushpushrelay.relay.NotificationFilter.Companion.DefaultPackages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppSettingsTest {

    private lateinit var settings: AppSettings

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().clear().commit()
        settings = AppSettings(context)
    }

    @Test
    fun `defaults to banking packages when nothing stored`() {
        assertEquals(DefaultPackages, settings.getAllowlist())
    }

    @Test
    fun `setAllowlist persists and updates flow`() {
        settings.setAllowlist(setOf("com.example.bank"))
        assertEquals(setOf("com.example.bank"), settings.allowlist.value)

        val reloaded = AppSettings(ApplicationProvider.getApplicationContext<Context>())
        assertEquals(setOf("com.example.bank"), reloaded.getAllowlist())
    }

    @Test
    fun `recordHeartbeat persists timestamp`() {
        settings.recordHeartbeat(1234567890L)
        val reloaded = AppSettings(ApplicationProvider.getApplicationContext<Context>())
        assertEquals(1234567890L, reloaded.lastHeartbeatAt.value)
    }

    @Test
    fun `recordRelay persists summary and timestamp`() {
        settings.recordRelay(42L, "PayPal: You sent €10")
        val reloaded = AppSettings(ApplicationProvider.getApplicationContext<Context>())
        val relay = reloaded.lastRelay.value
        assertEquals(42L, relay.timestampMillis)
        assertTrue(relay.summary!!.contains("PayPal"))
    }
}
