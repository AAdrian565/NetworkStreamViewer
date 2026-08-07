package com.adriant.networkstreamviewer.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adriant.networkstreamviewer.domain.model.AppSettings
import com.adriant.networkstreamviewer.domain.model.AppTheme
import com.adriant.networkstreamviewer.domain.model.DiscoveryRefreshInterval
import com.adriant.networkstreamviewer.domain.model.NdiBandwidth

@Composable
fun SettingsScreen(
    settings: AppSettings,
    developerOptionsUnlocked: Boolean,
    onThemeChanged: (AppTheme) -> Unit,
    onDefaultBandwidthChanged: (NdiBandwidth) -> Unit,
    onKeepScreenAwakeChanged: (Boolean) -> Unit,
    onShowPlaybackDiagnosticsChanged: (Boolean) -> Unit,
    onDeveloperModeChanged: (Boolean) -> Unit,
    onDiscoveryRefreshIntervalChanged: (DiscoveryRefreshInterval) -> Unit,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit
) {
    var themeDialogOpen by remember { mutableStateOf(false) }
    var bandwidthDialogOpen by remember { mutableStateOf(false) }
    var refreshDialogOpen by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsHeader(title = "Settings", onBack = onBack)
            SettingsSection("Appearance")
            ValueSetting(
                title = "Theme",
                summary = settings.theme.label,
                onClick = { themeDialogOpen = true }
            )
            HorizontalDivider()
            SettingsSection("Playback")
            ValueSetting(
                title = "Default quality",
                summary = settings.defaultBandwidth.qualityLabel,
                onClick = { bandwidthDialogOpen = true }
            )
            SwitchSetting(
                title = "Keep screen awake",
                summary = "Prevent the display from sleeping during playback.",
                checked = settings.keepScreenAwake,
                onCheckedChange = onKeepScreenAwakeChanged
            )
            SwitchSetting(
                title = "Show playback diagnostics",
                summary = "Show codec, bandwidth, resolution, FPS, and connection state.",
                checked = settings.showPlaybackDiagnostics,
                onCheckedChange = onShowPlaybackDiagnosticsChanged
            )
            HorizontalDivider()
            SettingsSection("Discovery")
            ValueSetting(
                title = "Refresh interval",
                summary = settings.discoveryRefreshInterval.label,
                onClick = { refreshDialogOpen = true }
            )
            if (developerOptionsUnlocked) {
                HorizontalDivider()
                SettingsSection("Developer")
                SwitchSetting(
                    title = "Developer mode",
                    summary = "Add a simulated empty stream that exposes player and PTZ UI.",
                    checked = settings.developerMode,
                    onCheckedChange = onDeveloperModeChanged
                )
            }
            HorizontalDivider()
            ValueSetting(
                title = "About",
                summary = "Version, NDI® attribution, and open-source licenses.",
                onClick = onOpenAbout
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (themeDialogOpen) {
        ChoiceDialog(
            title = "Theme",
            choices = AppTheme.entries,
            selected = settings.theme,
            label = AppTheme::label,
            onSelected = {
                onThemeChanged(it)
                themeDialogOpen = false
            },
            onDismiss = { themeDialogOpen = false }
        )
    }
    if (bandwidthDialogOpen) {
        ChoiceDialog(
            title = "Default quality",
            choices = NdiBandwidth.entries,
            selected = settings.defaultBandwidth,
            label = NdiBandwidth::qualityLabel,
            onSelected = {
                onDefaultBandwidthChanged(it)
                bandwidthDialogOpen = false
            },
            onDismiss = { bandwidthDialogOpen = false }
        )
    }
    if (refreshDialogOpen) {
        ChoiceDialog(
            title = "Discovery refresh interval",
            choices = DiscoveryRefreshInterval.entries,
            selected = settings.discoveryRefreshInterval,
            label = DiscoveryRefreshInterval::label,
            onSelected = {
                onDiscoveryRefreshIntervalChanged(it)
                refreshDialogOpen = false
            },
            onDismiss = { refreshDialogOpen = false }
        )
    }
}

private val NdiBandwidth.qualityLabel: String
    get() = when (this) {
        NdiBandwidth.AUTOMATIC -> "Automatic"
        NdiBandwidth.HIGHEST -> "Highest"
        NdiBandwidth.LOWEST -> "Preview / Low"
    }

@Composable
internal fun SettingsHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) { Text("Back") }
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun ValueSetting(title: String, summary: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))
        Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    choices: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                choices.forEach { choice ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(choice) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = choice == selected,
                            onClick = { onSelected(choice) }
                        )
                        Text(label(choice), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
