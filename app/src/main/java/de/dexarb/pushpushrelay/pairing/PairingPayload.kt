package de.dexarb.pushpushrelay.pairing

import java.net.URI
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class PairingPayload(
    val notifyUrl: String,
    val heartbeatUrl: String,
    val secret: String,
)

sealed class PairingParseResult {
    data class Parsed(val payload: PairingPayload) : PairingParseResult()
    data class Invalid(val reason: String) : PairingParseResult()
}

@Serializable
private data class WirePairingPayload(
    val v: Int,
    val type: String,
    @kotlinx.serialization.SerialName("notify_url") val notifyUrl: String,
    @kotlinx.serialization.SerialName("heartbeat_url") val heartbeatUrl: String,
    val secret: String,
)

object PairingPayloadParser {

    private const val EXPECTED_VERSION = 1
    private const val EXPECTED_TYPE = "pushpush-pairing"
    private const val MAX_SECRET_LENGTH = 256

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(raw: String): PairingParseResult {
        val wire = try {
            json.decodeFromString(WirePairingPayload.serializer(), raw)
        } catch (e: Exception) {
            return PairingParseResult.Invalid("malformed JSON: ${e.message}")
        }
        if (wire.v != EXPECTED_VERSION) {
            return PairingParseResult.Invalid("unsupported version ${wire.v}")
        }
        if (wire.type != EXPECTED_TYPE) {
            return PairingParseResult.Invalid("unexpected type '${wire.type}'")
        }
        if (!isHttpUrl(wire.notifyUrl)) {
            return PairingParseResult.Invalid("invalid notify_url")
        }
        if (!isHttpUrl(wire.heartbeatUrl)) {
            return PairingParseResult.Invalid("invalid heartbeat_url")
        }
        if (wire.secret.isBlank()) {
            return PairingParseResult.Invalid("blank secret")
        }
        if (wire.secret.length > MAX_SECRET_LENGTH) {
            return PairingParseResult.Invalid("secret longer than $MAX_SECRET_LENGTH chars")
        }
        return PairingParseResult.Parsed(
            PairingPayload(notifyUrl = wire.notifyUrl, heartbeatUrl = wire.heartbeatUrl, secret = wire.secret)
        )
    }

    private fun isHttpUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val uri = try {
            URI(url)
        } catch (e: Exception) {
            return false
        }
        return uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank()
    }
}
