package de.dexarb.pushpushrelay.heartbeat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.dexarb.pushpushrelay.PushRelayApp

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if ((context.applicationContext as PushRelayApp).configStore.load() != null) {
            HeartbeatService.start(context)
        }
    }
}
