package de.dexarb.pushpushrelay.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import de.dexarb.pushpushrelay.MainActivity
import de.dexarb.pushpushrelay.pairing.PairingParseResult
import de.dexarb.pushpushrelay.pairing.PairingPayloadParser
import de.dexarb.pushpushrelay.relay.NotificationAccess
import de.dexarb.pushpushrelay.storage.DeviceIdGenerator
import de.dexarb.pushpushrelay.storage.PairedConfig

@Composable
fun AppRoot(activity: MainActivity) {
    val context = LocalContext.current
    val app = activity.app
    val snackbar = remember { SnackbarHostState() }
    var paired by remember { mutableStateOf(app.configStore.load() != null) }

    LaunchedEffect(paired) {
        if (paired && !NotificationAccess.isEnabled(context)) {
            context.startActivity(NotificationAccess.settingsIntent())
        } else if (paired) {
            HeartbeatStarter.startHeartbeat(context)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        if (!paired) {
            ScanScreen(onQrScanned = { raw ->
                when (val result = PairingPayloadParser.parse(raw)) {
                    is PairingParseResult.Parsed -> {
                        app.configStore.save(
                            PairedConfig(
                                notifyUrl = result.payload.notifyUrl,
                                heartbeatUrl = result.payload.heartbeatUrl,
                                secret = result.payload.secret,
                                deviceId = DeviceIdGenerator.generate(),
                            )
                        )
                        paired = true
                    }
                    is PairingParseResult.Invalid -> {
                        paired = false
                    }
                }
            })
        } else {
            StatusScreen(app = app, onUnpair = {
                app.configStore.clear()
                paired = false
            }, modifier = Modifier.padding(padding))
        }
    }
}

private object HeartbeatStarter {

    fun startHeartbeat(context: android.content.Context) {
        de.dexarb.pushpushrelay.heartbeat.HeartbeatService.start(context)
    }
}
