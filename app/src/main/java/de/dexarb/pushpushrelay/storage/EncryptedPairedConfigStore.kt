package de.dexarb.pushpushrelay.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import de.dexarb.pushpushrelay.pairing.PaymentMethod
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class EncryptedPairedConfigStore(context: Context) : PairedConfigStore {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "paired_config",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val json = Json { ignoreUnknownKeys = true }

    override fun load(): PairedConfig? {
        val notifyUrl = prefs.getString(KEY_NOTIFY_URL, null) ?: return null
        val heartbeatUrl = prefs.getString(KEY_HEARTBEAT_URL, null) ?: return null
        val secret = prefs.getString(KEY_SECRET, null) ?: return null
        val deviceId = prefs.getString(KEY_DEVICE_ID, null) ?: return null
        val paymentMethods = prefs.getString(KEY_PAYMENT_METHODS, null)?.let { encoded ->
            runCatching {
                json.decodeFromString(ListSerializer(PaymentMethod.serializer()), encoded)
            }.getOrNull()
        }
        return PairedConfig(notifyUrl, heartbeatUrl, secret, deviceId, paymentMethods)
    }

    override fun save(config: PairedConfig) {
        prefs.edit()
            .putString(KEY_NOTIFY_URL, config.notifyUrl)
            .putString(KEY_HEARTBEAT_URL, config.heartbeatUrl)
            .putString(KEY_SECRET, config.secret)
            .putString(KEY_DEVICE_ID, config.deviceId)
            .putString(
                KEY_PAYMENT_METHODS,
                config.paymentMethods?.let {
                    json.encodeToString(ListSerializer(PaymentMethod.serializer()), it)
                },
            )
            .apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_NOTIFY_URL = "notify_url"
        const val KEY_HEARTBEAT_URL = "heartbeat_url"
        const val KEY_SECRET = "secret"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_PAYMENT_METHODS = "payment_methods"
    }
}
