package de.dexarb.pushpushrelay

import android.app.Application
import de.dexarb.pushpushrelay.net.RelayClient
import de.dexarb.pushpushrelay.storage.AppSettings
import de.dexarb.pushpushrelay.storage.EncryptedPairedConfigStore
import de.dexarb.pushpushrelay.storage.PairedConfigStore
import okhttp3.OkHttpClient

class PushRelayApp : Application() {

    lateinit var settings: AppSettings
        private set

    val configStore: PairedConfigStore by lazy { EncryptedPairedConfigStore(this) }

    val okHttpClient: OkHttpClient by lazy { RelayClient.defaultClient() }

    override fun onCreate() {
        super.onCreate()
        settings = AppSettings(this)
    }
}
