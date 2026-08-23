package de.dexarb.pushpushrelay.relay

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotificationAccessTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Settings.Secure.putString(
            context.contentResolver,
            "enabled_notification_listeners",
            "",
        )
    }

    @Test
    fun `disabled when listener not listed`() {
        assertFalse(NotificationAccess.isEnabled(context))
    }

    @Test
    fun `enabled when own package is listed`() {
        Settings.Secure.putString(
            context.contentResolver,
            "enabled_notification_listeners",
            "component:de.dexarb.pushpushrelay/de.dexarb.pushpushrelay.relay.PushRelayService",
        )
        assertTrue(NotificationAccess.isEnabled(context))
    }

    @Test
    fun `not enabled for other apps listeners`() {
        Settings.Secure.putString(
            context.contentResolver,
            "enabled_notification_listeners",
            "com.other.app/com.other.app.Listener",
        )
        assertFalse(NotificationAccess.isEnabled(context))
    }
}
