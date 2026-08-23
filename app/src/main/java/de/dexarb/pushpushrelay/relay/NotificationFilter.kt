package de.dexarb.pushpushrelay.relay

class NotificationFilter(private val allowlist: Set<String>) {

    fun shouldRelay(packageName: String): Boolean =
        packageName.isNotBlank() && allowlist.any { it.equals(packageName, ignoreCase = true) }

    companion object {
        val DefaultPackages: Set<String> = setOf(
            "com.paypal.android.p2pmobile",
            "com.revolut.revolut",
            "com.transferwise.android",
            "com.n26.android",
            "com.sparkasse.zhk",
            "com.dkbcodefactory.banking",
            "com.commerzbanking.app0",
            "com.db.pwcc",
        )
    }
}
