package de.dexarb.pushpushrelay.storage

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

data class LastRelay(val timestampMillis: Long, val summary: String?)

@Serializable
data class MessageLogEntry(
    val kind: String,
    val timestampMillis: Long,
    val summary: String?,
    val success: Boolean,
    val detail: String? = null,
    val appPackage: String? = null,
)

class AppSettings(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _allowlist = MutableStateFlow(readAllowlist())
    val allowlist: StateFlow<Set<String>> = _allowlist

    fun getAllowlist(): Set<String> = _allowlist.value

    fun setAllowlist(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_ALLOWLIST, packages).apply()
        _allowlist.value = packages.toSet()
    }

    private val _lastHeartbeatAt = MutableStateFlow(prefs.getLong(KEY_LAST_HEARTBEAT, 0L))
    val lastHeartbeatAt: StateFlow<Long> = _lastHeartbeatAt

    fun recordHeartbeat(timestampMillis: Long) {
        prefs.edit().putLong(KEY_LAST_HEARTBEAT, timestampMillis).apply()
        _lastHeartbeatAt.value = timestampMillis
    }

    private val _lastRelay = MutableStateFlow(readLastRelay())
    val lastRelay: StateFlow<LastRelay> = _lastRelay

    fun recordRelay(timestampMillis: Long, summary: String?) {
        prefs.edit()
            .putLong(KEY_LAST_RELAY_AT, timestampMillis)
            .putString(KEY_LAST_RELAY_SUMMARY, summary)
            .apply()
        _lastRelay.value = LastRelay(timestampMillis, summary)
    }

    private fun readAllowlist(): Set<String> =
        prefs.getStringSet(KEY_ALLOWLIST, null)?.toSet() ?: emptySet()

    private fun readLastRelay(): LastRelay =
        LastRelay(prefs.getLong(KEY_LAST_RELAY_AT, 0L), prefs.getString(KEY_LAST_RELAY_SUMMARY, null))

    private val json = Json { ignoreUnknownKeys = true }

    private val _messageLog = MutableStateFlow(readMessageLog())
    val messageLog: StateFlow<List<MessageLogEntry>> = _messageLog

    @Synchronized
    fun recordMessage(entry: MessageLogEntry) {
        val updated = (listOf(entry) + _messageLog.value).take(MAX_MESSAGE_LOG)
        prefs.edit()
            .putString(KEY_MESSAGE_LOG, json.encodeToString(ListSerializer(MessageLogEntry.serializer()), updated))
            .apply()
        _messageLog.value = updated
    }

    private fun readMessageLog(): List<MessageLogEntry> =
        runCatching {
            prefs.getString(KEY_MESSAGE_LOG, null)?.let {
                json.decodeFromString(ListSerializer(MessageLogEntry.serializer()), it)
            }
        }.getOrNull().orEmpty()

    private companion object {
        const val KEY_ALLOWLIST = "allowlist"
        const val KEY_LAST_HEARTBEAT = "last_heartbeat_at"
        const val KEY_LAST_RELAY_AT = "last_relay_at"
        const val KEY_LAST_RELAY_SUMMARY = "last_relay_summary"
        const val KEY_MESSAGE_LOG = "message_log"
        const val MAX_MESSAGE_LOG = 50
    }
}
