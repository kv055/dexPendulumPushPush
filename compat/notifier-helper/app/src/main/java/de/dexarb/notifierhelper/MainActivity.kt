package de.dexarb.notifierhelper

import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.os.Bundle

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notification = Notification.Builder(this)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("Payment received")
            .setContentText("You received 25,00 EUR from Compat Test")
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(1, notification)
        finish()
    }
}
