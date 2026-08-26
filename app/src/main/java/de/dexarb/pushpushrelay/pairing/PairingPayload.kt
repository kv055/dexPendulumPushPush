package de.dexarb.pushpushrelay.pairing

import java.net.URI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PaymentMethod(
    val id: Long = 0L,
    val name: String = "",
    val category: String = "",
    @SerialName("alias_list") val aliasList: List<String> = emptyList(),
    val sources: List<String> = emptyList(),
    val currencies: List<String> = emptyList(),
)

data class PairingPayload(
    val schemaVersion: Int,
    val notifyUrl: String,
    val heartbeatUrl: String,
    val secret: String,
    val paymentMethods: List<PaymentMethod>?,
) {
    override fun toString(): String =
        "PairingPayload(schemaVersion=$schemaVersion, notifyUrl=$notifyUrl, " +
            "heartbeatUrl=$heartbeatUrl, secret=REDACTED, paymentMethods=${paymentMethods?.size})"
}

sealed class PairingParseResult {
    data class Parsed(val payload: PairingPayload) : PairingParseResult()
    data class Invalid(val reason: String) : PairingParseResult()
}

@Serializable
private data class WirePairingPayload(
    val v: Int,
    val type: String,
    @SerialName("notify_url") val notifyUrl: String,
    @SerialName("heartbeat_url") val heartbeatUrl: String,
    val secret: String,
    @SerialName("payment_methods") val paymentMethods: List<PaymentMethod>? = null,
)

object PairingPayloadParser {

    private const val MIN_VERSION = 1
    private const val EXPECTED_TYPE = "pushpush-pairing"
    private const val MAX_SECRET_LENGTH = 256

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(raw: String): PairingParseResult {
        val wire = try {
            json.decodeFromString(WirePairingPayload.serializer(), raw)
        } catch (e: Exception) {
            return PairingParseResult.Invalid("malformed JSON: ${e.message}")
        }
        if (wire.v < MIN_VERSION) {
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
            PairingPayload(
                schemaVersion = wire.v,
                notifyUrl = wire.notifyUrl,
                heartbeatUrl = wire.heartbeatUrl,
                secret = wire.secret,
                // null means the payload carried no authorization data (v1);
                // an empty list is a valid v2 state where nothing is authorized yet.
                paymentMethods = wire.paymentMethods,
            )
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
