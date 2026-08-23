package de.dexarb.pushpushrelay.relay

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

object NotificationAccess {

    fun isEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?: return false
        return flat.split(":").any {
            ComponentName.unflattenFromString(it)?.packageName == context.packageName
        }
    }

    fun settingsIntent() = android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
}
