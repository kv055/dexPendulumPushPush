package de.dexarb.pushpushrelay.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedPairedConfigStore(context: Context) : PairedConfigStore {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "paired_config",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun load(): PairedConfig? {
        val notifyUrl = prefs.getString(KEY_NOTIFY_URL, null) ?: return null
        val heartbeatUrl = prefs.getString(KEY_HEARTBEAT_URL, null) ?: return null
        val secret = prefs.getString(KEY_SECRET, null) ?: return null
        val deviceId = prefs.getString(KEY_DEVICE_ID, null) ?: return null
        return PairedConfig(notifyUrl, heartbeatUrl, secret, deviceId)
    }

    override fun save(config: PairedConfig) {
        prefs.edit()
            .putString(KEY_NOTIFY_URL, config.notifyUrl)
            .putString(KEY_HEARTBEAT_URL, config.heartbeatUrl)
            .putString(KEY_SECRET, config.secret)
            .putString(KEY_DEVICE_ID, config.deviceId)
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
    }
}
