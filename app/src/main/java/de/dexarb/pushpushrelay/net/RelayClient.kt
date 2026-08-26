package de.dexarb.pushpushrelay.net

import de.dexarb.pushpushrelay.storage.MessageLogEntry
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed class RelayResult {
    object Success : RelayResult()

    data class HttpError(val code: Int) : RelayResult() {
        override fun toString(): String = "HttpError(code=$code)"
    }

    data class NetworkError(val message: String?) : RelayResult()
}

fun RelayResult.toLogEntry(kind: String, summary: String): MessageLogEntry = MessageLogEntry(
    kind = kind,
    timestampMillis = System.currentTimeMillis(),
    summary = summary,
    success = this is RelayResult.Success,
    detail = when (this) {
        is RelayResult.Success -> null
        is RelayResult.HttpError -> "HTTP $code"
        is RelayResult.NetworkError -> message?.let { "network: $it" } ?: "network error"
    },
)

class RelayClient(
    private val okHttpClient: OkHttpClient = defaultClient(),
    private val notifyUrl: String,
    private val heartbeatUrl: String,
    private val secretProvider: () -> String,
) {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        explicitNulls = false
        encodeDefaults = true
    }

    fun postNotification(app: String?, title: String?, text: String?): RelayResult =
        post(notifyUrl) {
            buildJsonObject {
                putIfNotNull("app", app)
                putIfNotNull("title", title)
                putIfNotNull("text", text)
            }
        }

    fun postHeartbeat(deviceId: String): RelayResult =
        post(heartbeatUrl) { buildJsonObject { put("device_id", deviceId.take(MAX_DEVICE_ID_LENGTH)) } }

    private fun post(url: String, body: () -> JsonObject): RelayResult {
        val payload = json.encodeToString(JsonObject.serializer(), body())
        val request = Request.Builder()
            .url(url)
            .header("X-Webhook-Secret", secretProvider())
            .post(payload.toByteArray().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return try {
            okHttpClient.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> RelayResult.Success
                    else -> RelayResult.HttpError(response.code)
                }
            }
        } catch (e: IOException) {
            RelayResult.NetworkError(e.message)
        }
    }

    companion object {
        private const val MAX_DEVICE_ID_LENGTH = 128
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private fun kotlinx.serialization.json.JsonObjectBuilder.putIfNotNull(
            key: String,
            value: String?,
        ) {
            if (value != null) put(key, value)
        }

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
