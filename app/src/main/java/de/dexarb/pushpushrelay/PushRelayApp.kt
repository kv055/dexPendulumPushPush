package de.dexarb.pushpushrelay

import android.app.Application
import de.dexarb.pushpushrelay.storage.AppSettings
import de.dexarb.pushpushrelay.storage.EncryptedPairedConfigStore
import de.dexarb.pushpushrelay.storage.PairedConfigStore

class PushRelayApp : Application() {

    lateinit var settings: AppSettings
        private set

    lateinit var configStore: PairedConfigStore
        private set

    override fun onCreate() {
        super.onCreate()
        settings = AppSettings(this)
        configStore = EncryptedPairedConfigStore(this)
    }
}
