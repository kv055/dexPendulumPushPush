package de.dexarb.pushpushrelay.storage

import java.security.SecureRandom

data class PairedConfig(
    val notifyUrl: String,
    val heartbeatUrl: String,
    val secret: String,
    val deviceId: String,
) {
    override fun toString(): String =
        "PairedConfig(notifyUrl=$notifyUrl, heartbeatUrl=$heartbeatUrl, secret=REDACTED, deviceId=$deviceId)"
}

interface PairedConfigStore {
    fun load(): PairedConfig?
    fun save(config: PairedConfig)
    fun clear()
}

object DeviceIdGenerator {
    private const val PREFIX = "payment-phone-"
    private const val HEX_CHARS = "0123456789abcdef"

    fun generate(): String {
        val random = SecureRandom()
        val suffix = CharArray(8) { HEX_CHARS[random.nextInt(16)] }
        return PREFIX + String(suffix)
    }
}
