package de.dexarb.pushpushrelay

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import de.dexarb.pushpushrelay.storage.AppSettings
import de.dexarb.pushpushrelay.ui.AppRoot
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    lateinit var app: PushRelayApp
        private set

    val pairRequest = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as PushRelayApp
        capturePairIntent(intent)
        setContent { AppRoot(this) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        capturePairIntent(intent)
    }

    private fun capturePairIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data?.host == "pair") {
            intent.data?.getQueryParameter("data")?.let { pairRequest.value = it }
        }
    }
}
