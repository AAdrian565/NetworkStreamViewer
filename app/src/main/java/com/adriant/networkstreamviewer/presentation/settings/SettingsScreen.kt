package com.adriant.networkstreamviewer.presentation.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.adriant.networkstreamviewer.domain.model.AppSettings
import com.adriant.networkstreamviewer.domain.model.AppTheme
import com.adriant.networkstreamviewer.domain.model.DiscoveryRefreshInterval
import com.adriant.networkstreamviewer.domain.model.NdiBandwidth
import com.adriant.networkstreamviewer.ui.theme.ndiMonitorColors
import java.util.Locale

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
    onBack: () -> Unit,
) {
    var themeDialogOpen by remember { mutableStateOf(false) }
    var bandwidthDialogOpen by remember { mutableStateOf(false) }
    var refreshDialogOpen by remember { mutableStateOf(false) }
    val colors = ndiMonitorColors()

    Scaffold(
        containerColor = colors.background,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .background(colors.background),
        ) {
            SettingsHeader(title = "Settings", onBack = onBack)

            SettingsSection("Appearance") {
                ValueSetting(
                    title = "Theme",
                    summary = settings.theme.label,
                    onClick = { themeDialogOpen = true },
                )
            }

            SettingsSection("Playback") {
                ValueSetting(
                    title = "Default quality",
                    summary = settings.defaultBandwidth.qualityLabel,
                    onClick = { bandwidthDialogOpen = true },
                )
                SwitchSetting(
                    title = "Keep screen awake",
                    summary = "Prevent the display from sleeping during playback.",
                    checked = settings.keepScreenAwake,
                    onCheckedChange = onKeepScreenAwakeChanged,
                )
                SwitchSetting(
                    title = "Show playback diagnostics",
                    summary = "Show codec, bandwidth, resolution, FPS, and connection state.",
                    checked = settings.showPlaybackDiagnostics,
                    onCheckedChange = onShowPlaybackDiagnosticsChanged,
                )
            }

            SettingsSection("Discovery") {
                ValueSetting(
                    title = "Refresh interval",
                    summary = settings.discoveryRefreshInterval.label,
                    onClick = { refreshDialogOpen = true },
                )
            }

            if (developerOptionsUnlocked) {
                SettingsSection("Developer") {
                    SwitchSetting(
                        title = "Developer mode",
                        summary = "Add a simulated empty stream that exposes player and PTZ UI.",
                        checked = settings.developerMode,
                        onCheckedChange = onDeveloperModeChanged,
                    )
                }
            }

            SettingsSection("Application") {
                ValueSetting(
                    title = "About",
                    summary = "Version, NDI® attribution, and open-source licenses.",
                    onClick = onOpenAbout,
                )
            }
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
            onDismiss = { themeDialogOpen = false },
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
            onDismiss = { bandwidthDialogOpen = false },
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
            onDismiss = { refreshDialogOpen = false },
        )
    }
}

@Composable
internal fun SettingsHeader(
    title: String,
    onBack: () -> Unit,
) {
    val colors = ndiMonitorColors()
    Column(
        modifier = Modifier.fillMaxWidth().background(colors.topBar),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.settingsButton)
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack)
                        .semantics {
                            contentDescription = "Back"
                            role = Role.Button
                        },
                contentAlignment = Alignment.Center,
            ) {
                SettingsBackIcon(color = colors.primaryText)
            }
            Spacer(Modifier.size(10.dp))
            Box(
                modifier = Modifier.size(7.dp).background(colors.accent, CircleShape),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = title.uppercase(Locale.ROOT),
                color = colors.primaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.1.sp,
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
    }
}

@Composable
private fun SettingsBackIcon(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val strokeWidth = 1.8.dp.toPx()
        val centerY = size.height / 2f
        val left = size.width * 0.18f
        val right = size.width * 0.8f
        drawLine(color, Offset(left, centerY), Offset(right, centerY), strokeWidth, StrokeCap.Round)
        drawLine(
            color,
            Offset(left, centerY),
            Offset(size.width * 0.46f, size.height * 0.18f),
            strokeWidth,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(left, centerY),
            Offset(size.width * 0.46f, size.height * 0.82f),
            strokeWidth,
            StrokeCap.Round,
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val colors = ndiMonitorColors()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(6.dp).background(colors.accent, CircleShape))
            Spacer(Modifier.size(8.dp))
            Text(
                text = title.uppercase(Locale.ROOT),
                color = colors.primaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun ValueSetting(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    val colors = ndiMonitorColors()
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .clip(shape)
                .background(colors.sourceUpper)
                .border(1.dp, colors.border, shape)
                .clickable(onClick = onClick)
                .semantics {
                    contentDescription = "$title: $summary"
                    role = Role.Button
                }.padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = summary,
                color = colors.secondaryText,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
        Text(
            text = "›",
            color = colors.accent,
            fontSize = 25.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = ndiMonitorColors()
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .clip(shape)
                .background(colors.sourceUpper)
                .border(1.dp, colors.border, shape)
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.primaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = summary,
                color = colors.secondaryText,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
        SettingsToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun SettingsToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ndiMonitorColors()
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier =
            modifier
                .size(width = 44.dp, height = 24.dp)
                .clip(shape)
                .background(if (checked) colors.accent.copy(alpha = 0.35f) else colors.sourceLower)
                .border(1.dp, if (checked) colors.accent.copy(alpha = 0.75f) else colors.border, shape)
                .semantics {
                    contentDescription = "Toggle setting"
                    role = Role.Switch
                }.toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
    ) {
        Box(
            modifier =
                Modifier
                    .padding(3.dp)
                    .size(16.dp)
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                    .background(if (checked) colors.accent else colors.secondaryText, CircleShape),
        )
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    choices: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ndiMonitorColors()
    val shape = RoundedCornerShape(16.dp)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
                    .clip(shape)
                    .background(colors.sourceUpper)
                    .border(1.dp, colors.border, shape)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(6.dp).background(colors.accent, CircleShape))
                Spacer(Modifier.size(8.dp))
                Text(
                    text = title.uppercase(Locale.ROOT),
                    color = colors.primaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.1.sp,
                )
            }
            choices.forEach { choice ->
                ChoiceRow(
                    label = label(choice),
                    selected = choice == selected,
                    onClick = { onSelected(choice) },
                )
            }
            Text(
                text = "CANCEL",
                color = colors.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier =
                    Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                        .semantics {
                            contentDescription = "Cancel"
                            role = Role.Button
                        },
            )
        }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = ndiMonitorColors()
    val rowShape = RoundedCornerShape(10.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(rowShape)
                .background(if (selected) colors.accent.copy(alpha = 0.08f) else Color.Transparent)
                .clickable(onClick = onClick)
                .semantics {
                    contentDescription = label
                    role = Role.RadioButton
                }.padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(19.dp)
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) colors.accent else colors.secondaryText,
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(modifier = Modifier.size(8.dp).background(colors.accent, CircleShape))
            }
        }
        Text(
            text = label,
            color = colors.primaryText,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

private val NdiBandwidth.qualityLabel: String
    get() =
        when (this) {
            NdiBandwidth.AUTOMATIC -> "Automatic"
            NdiBandwidth.HIGHEST -> "Highest"
            NdiBandwidth.LOWEST -> "Preview / Low"
        }
