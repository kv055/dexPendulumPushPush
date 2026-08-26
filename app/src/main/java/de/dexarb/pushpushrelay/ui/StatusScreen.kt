package de.dexarb.pushpushrelay.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import de.dexarb.pushpushrelay.PushRelayApp
import de.dexarb.pushpushrelay.R
import de.dexarb.pushpushrelay.net.RelayClient
import de.dexarb.pushpushrelay.net.RelayResult
import de.dexarb.pushpushrelay.net.toLogEntry
import de.dexarb.pushpushrelay.pairing.PaymentMethod
import de.dexarb.pushpushrelay.storage.MessageLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable
fun StatusScreen(app: PushRelayApp, onUnpair: () -> Unit, modifier: Modifier = Modifier) {
    val config = remember { app.configStore.load() }
    val allowlist by app.settings.allowlist.collectAsState()
    val lastHeartbeatAt by app.settings.lastHeartbeatAt.collectAsState()
    val lastRelay by app.settings.lastRelay.collectAsState()
    val messageLog by app.settings.messageLog.collectAsState()

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

        TestConnectionButton(app)

        Button(onClick = onUnpair) { Text("Reset pairing") }

        MessageLogSection(messageLog)

        AllowlistEditor(app, allowlist)

        SetupChecklist(config?.paymentMethods)
    }
}

@Composable
private fun TestConnectionButton(app: PushRelayApp) {
    val scope = rememberCoroutineScope()
    var testing by remember { mutableStateOf(false) }
    Button(
        onClick = {
            testing = true
            scope.launch {
                try {
                    val config = app.configStore.load() ?: return@launch
                    val client = RelayClient(
                        okHttpClient = app.okHttpClient,
                        notifyUrl = config.notifyUrl,
                        heartbeatUrl = config.heartbeatUrl,
                        secretProvider = { config.secret },
                    )
                    val result = withContext(Dispatchers.IO) { client.postHeartbeat(config.deviceId) }
                    if (result is RelayResult.Success) {
                        app.settings.recordHeartbeat(System.currentTimeMillis())
                    }
                    app.settings.recordMessage(result.toLogEntry(kind = "heartbeat", summary = "Test heartbeat"))
                } finally {
                    testing = false
                }
            }
        },
        enabled = !testing && app.configStore.load() != null,
    ) {
        Text(if (testing) "Testing…" else "Test connection")
    }
}

@Composable
private fun MessageLogSection(messageLog: List<MessageLogEntry>) {
    var expanded by remember { mutableStateOf(false) }
    var showHeartbeats by remember { mutableStateOf(true) }
    var showRelay by remember { mutableStateOf(true) }
    val relayApps = remember(messageLog) {
        messageLog.mapNotNull { it.appPackage }.distinct()
    }
    var appFilter by remember { mutableStateOf<Set<String>>(emptySet()) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Messages", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Show")
                }
            }
            if (!expanded) {
                Text("No heartbeat or relay activity yet.", style = MaterialTheme.typography.bodySmall)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = showHeartbeats,
                        onClick = { showHeartbeats = !showHeartbeats },
                        label = { Text("Heartbeats") },
                    )
                    FilterChip(
                        selected = showRelay,
                        onClick = { showRelay = !showRelay },
                        label = { Text("Relay") },
                    )
                }
                if (showRelay && relayApps.isNotEmpty()) {
                    Text("Filter relay apps", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        relayApps.forEach { pkg ->
                            FilterChip(
                                selected = pkg in appFilter,
                                onClick = {
                                    appFilter = if (pkg in appFilter) appFilter - pkg else appFilter + pkg
                                },
                                label = { Text(pkg.substringBeforeLast('.')) },
                            )
                        }
                    }
                }
                val visible = messageLog.filter { entry ->
                    when (entry.kind) {
                        "heartbeat" -> showHeartbeats
                        "relay" -> showRelay && (appFilter.isEmpty() || entry.appPackage in appFilter)
                        else -> true
                    }
                }
                if (visible.isEmpty()) {
                    Text("No messages match the current filters.", style = MaterialTheme.typography.bodySmall)
                } else {
                    visible.forEach { entry -> MessageLogRow(entry) }
                }
            }
        }
    }
}

@Composable
private fun MessageLogRow(entry: MessageLogEntry) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            if (entry.success) "✓" else "✗",
            color = if (entry.success) Color(0xFF2E7D32) else Color(0xFFC62828),
            style = MaterialTheme.typography.titleMedium,
        )
        Column {
            Text("${entry.kind}: ${entry.summary ?: ""}".trim())
            entry.detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Text(formatTime(entry.timestampMillis), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AllowlistEditor(app: PushRelayApp, allowlist: Set<String>) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var draft by remember(allowlist) { mutableStateOf(allowlist) }
    var query by remember { mutableStateOf("") }
    val installedApps = remember { installedApps(context) }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Relay notifications from", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = {
                if (expanded) {
                    draft = allowlist
                    query = ""
                }
                expanded = !expanded
            }) {
                Text(if (expanded) "Collapse" else if (allowlist.isEmpty()) "Select apps" else "Change")
            }
        }
        if (!expanded) {
            if (allowlist.isEmpty()) {
                Text(
                    "Nothing is relayed until you select at least one app.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                val selectedApps = remember(installedApps, allowlist) {
                    installedApps.filter { it.packageName in allowlist }
                }
                selectedApps.forEach { appInfo -> AppRow(appInfo, checked = true, onCheckedChange = null) }
            }
        } else {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search ${installedApps.size} apps") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
            val visible = remember(query, installedApps) {
                if (query.isBlank()) installedApps
                else installedApps.filter {
                    it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
                }
            }
            Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                visible.forEach { appInfo ->
                    AppRow(
                        appInfo = appInfo,
                        checked = appInfo.packageName in draft,
                        onCheckedChange = { checked ->
                            draft = if (checked) draft + appInfo.packageName else draft - appInfo.packageName
                        },
                    )
                }
            }
            Button(
                onClick = {
                    app.settings.setAllowlist(draft)
                    expanded = false
                    query = ""
                },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Confirm selection (${draft.size})")
            }
        }
    }
}

@Composable
private fun AppRow(appInfo: InstalledApp, checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
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

private fun installedApps(context: android.content.Context): List<InstalledApp> =
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
                    drawableToImageBitmap(
                        context.packageManager.getApplicationIcon(info.packageName)
                    )
                }.getOrDefault(null),
            )
        }
        .sortedBy { it.label.lowercase() }

private data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
)

private fun drawableToImageBitmap(drawable: android.graphics.drawable.Drawable): ImageBitmap {
    val size = 96
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, size, size)
    drawable.draw(canvas)
    return bitmap.asImageBitmap()
}

@Composable
private fun SetupChecklist(paymentMethods: List<PaymentMethod>?) {
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
            Text("2. Select the payment apps whose notifications should be relayed")
            Text("3. Keep the app installed; services restart after reboot")

            Text(
                "Authorized payment providers",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            when {
                paymentMethods == null -> Text(
                    "Paired with an older QR code — every allowlisted app is relayed.",
                    style = MaterialTheme.typography.bodySmall,
                )
                paymentMethods.isEmpty() -> Text(
                    "None yet — link and activate payment methods in the dashboard, " +
                        "then scan the pairing QR again to refresh this list.",
                    style = MaterialTheme.typography.bodySmall,
                )
                else -> {
                    paymentMethods.forEach { method ->
                        val details = listOfNotNull(
                            method.category.takeIf { it.isNotBlank() },
                            method.currencies.takeIf { it.isNotEmpty() }?.joinToString("/"),
                        ).joinToString(" · ")
                        Text(
                            if (details.isBlank()) "• ${method.name}"
                            else "• ${method.name} ($details)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(
                        "Only notifications mentioning these providers (or their aliases) are relayed.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String =
    if (millis == 0L) "never" else DateFormat.getDateTimeInstance().format(Date(millis))
