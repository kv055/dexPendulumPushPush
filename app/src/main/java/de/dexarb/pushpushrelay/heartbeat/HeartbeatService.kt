package de.dexarb.pushpushrelay.heartbeat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import de.dexarb.pushpushrelay.PushRelayApp
import de.dexarb.pushpushrelay.net.RelayClient
import de.dexarb.pushpushrelay.net.RelayResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class HeartbeatService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as PushRelayApp
        val config = app.configStore.load() ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        loopJob?.cancel()
        loopJob = scope.launch {
            val client = RelayClient(
                okHttpClient = app.okHttpClient,
                notifyUrl = config.notifyUrl,
                heartbeatUrl = config.heartbeatUrl,
                secretProvider = { config.secret },
            )
            HeartbeatLoop().run(
                action = { client.postHeartbeat(config.deviceId) is RelayResult.Success },
                onResult = { ok ->
                    if (ok) app.settings.recordHeartbeat(System.currentTimeMillis())
                },
            )
        }
        return START_STICKY
    }

    override fun onDestroy() {
        loopJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun startInForeground() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Heartbeat", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("PushPush relay active")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "heartbeat"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            val intent = Intent(context, HeartbeatService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: IllegalStateException) {
            }
        }
    }
}
