package de.dexarb.pushpushrelay.relay

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import de.dexarb.pushpushrelay.PushRelayApp
import de.dexarb.pushpushrelay.heartbeat.HeartbeatService
import de.dexarb.pushpushrelay.net.RelayClient
import de.dexarb.pushpushrelay.net.RelayResult
import de.dexarb.pushpushrelay.net.toLogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PushRelayService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        startServiceSafely(Intent(this, HeartbeatService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val app = application as PushRelayApp
        if (!NotificationFilter(app.settings.getAllowlist()).shouldRelay(sbn.packageName)) return
        val config = app.configStore.load() ?: return
        val title = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = listOfNotNull(
            sbn.notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
        ).firstOrNull { it.isNotBlank() }
        val appName = appNameOf(this, sbn.packageName)
        val authorizedMethods = config.paymentMethods
        if (authorizedMethods != null &&
            PaymentMethodMatcher(authorizedMethods).match(appName, title, text) == null
        ) {
            return
        }
        scope.launch {
            val client = RelayClient(
                okHttpClient = app.okHttpClient,
                notifyUrl = config.notifyUrl,
                heartbeatUrl = config.heartbeatUrl,
                secretProvider = { config.secret },
            )
            val result = client.postNotification(appName, title, text)
            if (result is RelayResult.Success) {
                app.settings.recordRelay(System.currentTimeMillis(), "$appName: ${title ?: ""}".trim())
            }
            app.settings.recordMessage(
                result.toLogEntry(kind = "relay", summary = "$appName: ${title ?: ""}".trim())
                    .copy(appPackage = sbn.packageName)
            )
        }
    }

    private fun startServiceSafely(intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (_: IllegalStateException) {
        }
    }

    private fun appNameOf(context: Context, packageName: String): String =
        runCatching {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        }.getOrDefault(packageName)
}
