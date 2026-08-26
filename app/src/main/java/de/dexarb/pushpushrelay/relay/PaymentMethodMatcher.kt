package de.dexarb.pushpushrelay.relay

import de.dexarb.pushpushrelay.pairing.PaymentMethod

class PaymentMethodMatcher(private val methods: List<PaymentMethod>) {

    fun match(appLabel: String?, title: String?, text: String?): PaymentMethod? {
        val haystack = listOfNotNull(appLabel, title, text)
            .joinToString("\n")
            .lowercase()
        if (haystack.isBlank()) return null
        return methods.firstOrNull { method ->
            (listOf(method.name) + method.aliasList)
                .filter { it.isNotBlank() }
                .any { haystack.contains(it.trim().lowercase()) }
        }
    }
}
