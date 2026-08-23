package de.dexarb.pushpushrelay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import de.dexarb.pushpushrelay.ui.AppRoot

class MainActivity : ComponentActivity() {

    lateinit var app: PushRelayApp
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as PushRelayApp
        setContent { AppRoot(this) }
    }
}
