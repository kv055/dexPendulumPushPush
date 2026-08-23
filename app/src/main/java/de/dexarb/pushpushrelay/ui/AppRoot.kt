package de.dexarb.pushpushrelay.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import de.dexarb.pushpushrelay.MainActivity
import de.dexarb.pushpushrelay.heartbeat.HeartbeatService
import de.dexarb.pushpushrelay.pairing.PairingParseResult
import de.dexarb.pushpushrelay.pairing.PairingPayloadParser
import de.dexarb.pushpushrelay.relay.NotificationAccess
import de.dexarb.pushpushrelay.storage.DeviceIdGenerator
import de.dexarb.pushpushrelay.storage.PairedConfig
import kotlinx.coroutines.launch

@Composable
fun AppRoot(activity: MainActivity) {
    val context = LocalContext.current
    val app = activity.app
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var paired by remember { mutableStateOf(app.configStore.load() != null) }
    var notificationAccessGranted by remember {
        mutableStateOf(paired && NotificationAccess.isEnabled(context))
    }

    val postNotificationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun syncPairedState() {
        if (!paired) return
        notificationAccessGranted = NotificationAccess.isEnabled(context)
        if (notificationAccessGranted) {
            HeartbeatService.start(context)
        } else {
            context.startActivity(NotificationAccess.settingsIntent())
        }
    }

    LaunchedEffect(paired) {
        if (!paired) return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        syncPairedState()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (paired) {
            notificationAccessGranted = NotificationAccess.isEnabled(context)
            if (notificationAccessGranted) HeartbeatService.start(context)
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
                        true
                    }
                    is PairingParseResult.Invalid -> {
                        scope.launch { snackbar.showSnackbar("Invalid QR: ${result.reason}") }
                        false
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
