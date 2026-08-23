package de.dexarb.pushpushrelay.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.dexarb.pushpushrelay.PushRelayApp
import de.dexarb.pushpushrelay.relay.NotificationFilter
import java.text.DateFormat
import java.util.Date

@Composable
fun StatusScreen(app: PushRelayApp, onUnpair: () -> Unit, modifier: Modifier = Modifier) {
    val config = remember { app.configStore.load() }
    val allowlist by app.settings.allowlist.collectAsState()
    val lastHeartbeatAt by app.settings.lastHeartbeatAt.collectAsState()
    val lastRelay by app.settings.lastRelay.collectAsState()

    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

        Text("Relay these apps", style = MaterialTheme.typography.titleMedium)
        NotificationFilter.DefaultPackages.forEach { pkg ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = pkg in allowlist,
                    onCheckedChange = { checked ->
                        app.settings.setAllowlist(if (checked) allowlist + pkg else allowlist - pkg)
                    },
                )
                Text(pkg)
            }
        }

        Button(onClick = onUnpair) { Text("Reset pairing") }
        SetupChecklist()
    }
}

@Composable
private fun SetupChecklist() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Setup checklist", style = MaterialTheme.typography.titleMedium)
            Text("1. Grant notification access when prompted")
            Text("2. Disable battery optimization for this app")
            Text("3. Keep the app installed; services restart after reboot")
        }
    }
}

private fun formatTime(millis: Long): String =
    if (millis == 0L) "never" else DateFormat.getDateTimeInstance().format(Date(millis))
