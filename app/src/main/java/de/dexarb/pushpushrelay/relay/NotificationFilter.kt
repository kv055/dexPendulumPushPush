package de.dexarb.pushpushrelay.relay

class NotificationFilter(private val allowlist: Set<String>) {

    fun shouldRelay(packageName: String): Boolean =
        packageName.isNotBlank() && allowlist.any { it.equals(packageName, ignoreCase = true) }
}
