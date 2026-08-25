package de.dexarb.pushpushrelay.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import de.dexarb.pushpushrelay.PushRelayApp
import de.dexarb.pushpushrelay.R
import java.text.DateFormat
import java.util.Date

@Composable
fun StatusScreen(app: PushRelayApp, onUnpair: () -> Unit, modifier: Modifier = Modifier) {
    val config = remember { app.configStore.load() }
    val allowlist by app.settings.allowlist.collectAsState()
    val lastHeartbeatAt by app.settings.lastHeartbeatAt.collectAsState()
    val lastRelay by app.settings.lastRelay.collectAsState()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(
                    if (isSystemInDarkTheme()) R.drawable.brand_mark_dark else R.drawable.brand_mark_light
                ),
                contentDescription = "DexArb pendulum mark",
                modifier = Modifier.size(56.dp),
            )
            Column {
                Text("PushPush Relay", style = MaterialTheme.typography.titleLarge)
                Text("Fiat payment signal relay", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Paired", style = MaterialTheme.typography.titleMedium)
                Text(config?.heartbeatUrl ?: "not paired")
                Text(
                    "Last heartbeat: ${formatTime(lastHeartbeatAt)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Last relay: ${lastRelay?.summary ?: "none"} (${formatTime(lastRelay?.timestampMillis ?: 0L)})",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        AllowlistEditor(app, allowlist)

        Button(onClick = onUnpair) { Text("Reset pairing") }
        SetupChecklist()
    }
}

@Composable
private fun AllowlistEditor(app: PushRelayApp, allowlist: Set<String>) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val installedApps = remember {
        context.packageManager.getInstalledApplications(0)
            .filter { info ->
                info.packageName != context.packageName &&
                    (
                        (info.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                            (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                        )
            }
            .map { info ->
                InstalledApp(
                    packageName = info.packageName,
                    label = runCatching {
                        context.packageManager.getApplicationLabel(info).toString()
                    }.getOrDefault(info.packageName),
                    icon = runCatching {
                        (context.packageManager.getApplicationIcon(info.packageName) as BitmapDrawable)
                            .toBitmap()
                            .asImageBitmap()
                    }.getOrDefault(null),
                )
            }
            .sortedBy { it.label.lowercase() }
    }
    val visible = remember(query, installedApps) {
        if (query.isBlank()) installedApps
        else installedApps.filter {
            it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text("Relay notifications from", style = MaterialTheme.typography.titleMedium)
        Text(
            "Nothing is relayed until you select at least one app.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search ${installedApps.size} apps") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
        Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            visible.forEach { appInfo ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = appInfo.packageName in allowlist,
                        onCheckedChange = { checked ->
                            app.settings.setAllowlist(if (checked) allowlist + appInfo.packageName else allowlist - appInfo.packageName)
                        },
                    )
                    appInfo.icon?.let { bitmap ->
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp).padding(end = 4.dp),
                        )
                    }
                    Column {
                        Text(appInfo.label, style = MaterialTheme.typography.bodyMedium)
                        Text(appInfo.packageName, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
)

@Composable
private fun SetupChecklist() {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Setup checklist", style = MaterialTheme.typography.titleMedium)
            val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
            val ignoring = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            if (!ignoring) {
                Button(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            .setData(Uri.parse("package:${context.packageName}"))
                    )
                }) { Text("Disable battery optimization") }
            } else {
                Text("Battery optimization: disabled ✓", style = MaterialTheme.typography.bodySmall)
            }
            Text("1. Grant notification access when prompted")
            Text("2. Keep the app installed; services restart after reboot")
        }
    }
}

private fun formatTime(millis: Long): String =
    if (millis == 0L) "never" else DateFormat.getDateTimeInstance().format(Date(millis))
