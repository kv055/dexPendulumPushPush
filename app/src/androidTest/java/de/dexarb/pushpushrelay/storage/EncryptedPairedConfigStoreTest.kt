package de.dexarb.pushpushrelay.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedPairedConfigStoreTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = EncryptedPairedConfigStore(context)

    @Test
    fun loadOnFreshInstallReturnsNull() {
        store.clear()
        assertNull(store.load())
    }

    @Test
    fun saveThenLoadRoundTripsAllFields() {
        store.clear()
        val config = PairedConfig(
            notifyUrl = "https://example.invalid/notify",
            heartbeatUrl = "https://example.invalid/heartbeat",
            secret = "s3cret-value",
            deviceId = "payment-phone-0123abcd",
        )
        store.save(config)
        assertEquals(config, store.load())
    }

    @Test
    fun secondSaveOverwritesFirst() {
        store.clear()
        val first = PairedConfig(
            "https://a.invalid/n", "https://a.invalid/h", "secret-a", "payment-phone-aaaaaaaa",
        )
        val second = PairedConfig(
            "https://b.invalid/n", "https://b.invalid/h", "secret-b", "payment-phone-bbbbbbbb",
        )
        store.save(first)
        store.save(second)
        assertEquals(second, store.load())
    }

    @Test
    fun clearRemovesConfig() {
        store.save(
            PairedConfig(
                "https://c.invalid/n", "https://c.invalid/h", "secret-c", "payment-phone-cccccccc",
            )
        )
        store.clear()
        assertNull(store.load())
    }

    @Test
    fun secretIsNotStoredInPlaintext() {
        val secret = "plaintext-secret-marker-12345"
        store.save(
            PairedConfig(
                "https://d.invalid/n", "https://d.invalid/h", secret, "payment-phone-dddddddd",
            )
        )

        val prefsFile = File(context.applicationInfo.dataDir, "shared_prefs/paired_config.xml")
        assertTrue(prefsFile.exists())
        val raw = prefsFile.readText()
        assertFalse(raw.contains(secret))
    }
}
