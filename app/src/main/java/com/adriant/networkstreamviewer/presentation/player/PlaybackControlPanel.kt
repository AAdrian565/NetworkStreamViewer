package com.adriant.networkstreamviewer.presentation.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adriant.networkstreamviewer.domain.model.NdiAudioStatus
import com.adriant.networkstreamviewer.domain.model.NdiBandwidth
import com.adriant.networkstreamviewer.ui.theme.ndiMonitorColors

@Composable
internal fun PlaybackControlPanel(
    isVisible: Boolean,
    bandwidth: NdiBandwidth,
    audioStatus: NdiAudioStatus,
    audioVolume: Float,
    audioMuted: Boolean,
    showAudioDbLabels: Boolean,
    onBandwidthChanged: (NdiBandwidth) -> Unit,
    onAudioMutedChanged: (Boolean) -> Unit,
    onAudioVolumeChanged: (Float) -> Unit,
    onShowAudioDbLabelsChanged: (Boolean) -> Unit,
    onRetryAudioFocus: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(PlaybackTab.VIDEO) }
    val colors = ndiMonitorColors()
    val panelShape = RoundedCornerShape(16.dp)

    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.96f),
        exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.96f),
    ) {
        Column(
            modifier =
                Modifier
                    .widthIn(min = 360.dp, max = 420.dp)
                    .clip(panelShape)
                    .background(Color(0xF70A0A10), panelShape)
                    .border(1.dp, colors.border, panelShape)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .semantics { contentDescription = "Playback control panel" },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(36.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "▣",
                    color = colors.accent,
                    fontSize = 14.sp,
                )
                Text(
                    text = "PLAYBACK",
                    color = colors.primaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.1.sp,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                OverlayCloseButton(
                    contentDescription = "Hide playback controls",
                    onClick = onClose,
                )
            }

            PlaybackTabSelector(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )

            when (selectedTab) {
                PlaybackTab.VIDEO ->
                    VideoQualitySection(
                        selectedBandwidth = bandwidth,
                        onBandwidthChanged = onBandwidthChanged,
                    )
                PlaybackTab.AUDIO ->
                    AudioSettingsSection(
                        audioStatus = audioStatus,
                        audioVolume = audioVolume,
                        audioMuted = audioMuted,
                        showAudioDbLabels = showAudioDbLabels,
                        onAudioMutedChanged = onAudioMutedChanged,
                        onAudioVolumeChanged = onAudioVolumeChanged,
                        onShowAudioDbLabelsChanged = onShowAudioDbLabelsChanged,
                        onRetryAudioFocus = onRetryAudioFocus,
                    )
            }
        }
    }
}

@Composable
private fun PlaybackTabSelector(
    selectedTab: PlaybackTab,
    onTabSelected: (PlaybackTab) -> Unit,
) {
    val colors = ndiMonitorColors()
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(shape)
                .background(colors.sourceLower)
                .border(1.dp, colors.border, shape),
    ) {
        PlaybackTabButton(
            label = "Video",
            selected = selectedTab == PlaybackTab.VIDEO,
            onClick = { onTabSelected(PlaybackTab.VIDEO) },
            modifier = Modifier.weight(1f).fillMaxSize(),
        )
        PlaybackTabButton(
            label = "Audio",
            selected = selectedTab == PlaybackTab.AUDIO,
            onClick = { onTabSelected(PlaybackTab.AUDIO) },
            modifier = Modifier.weight(1f).fillMaxSize(),
        )
    }
}

@Composable
private fun PlaybackTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ndiMonitorColors()
    Box(
        modifier =
            modifier
                .clickable(onClick = onClick)
                .semantics {
                    contentDescription = "$label playback tab"
                    role = Role.Tab
                },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) colors.accent else colors.secondaryText,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
        if (selected) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.42f)
                        .height(2.dp)
                        .background(colors.accent),
            )
        }
    }
}

@Composable
private fun VideoQualitySection(
    selectedBandwidth: NdiBandwidth,
    onBandwidthChanged: (NdiBandwidth) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionLabel("VIDEO QUALITY")
        NdiBandwidth.entries.forEach { option ->
            PlaybackQualityOption(
                bandwidth = option,
                selected = selectedBandwidth == option,
                onClick = { onBandwidthChanged(option) },
            )
        }
    }
}

@Composable
private fun PlaybackQualityOption(
    bandwidth: NdiBandwidth,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = ndiMonitorColors()
    val rowShape = RoundedCornerShape(10.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(rowShape)
                .background(if (selected) colors.accent.copy(alpha = 0.08f) else Color.Transparent)
                .clickable(onClick = onClick)
                .semantics {
                    contentDescription = "Video quality: ${bandwidth.label}"
                    role = Role.RadioButton
                }.padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaybackRadioIndicator(selected = selected)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = bandwidth.label,
                color = if (selected) colors.primaryText else colors.primaryText.copy(alpha = 0.88f),
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            )
            Text(
                text = bandwidth.description,
                color = colors.secondaryText,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
private fun PlaybackRadioIndicator(selected: Boolean) {
    val colors = ndiMonitorColors()
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
            Box(
                modifier = Modifier.size(8.dp).background(colors.accent, CircleShape),
            )
        }
    }
}

@Composable
private fun AudioSettingsSection(
    audioStatus: NdiAudioStatus,
    audioVolume: Float,
    audioMuted: Boolean,
    showAudioDbLabels: Boolean,
    onAudioMutedChanged: (Boolean) -> Unit,
    onAudioVolumeChanged: (Float) -> Unit,
    onShowAudioDbLabelsChanged: (Boolean) -> Unit,
    onRetryAudioFocus: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("AUDIO LEVEL")
        AudioControls(
            status = audioStatus,
            volume = audioVolume,
            muted = audioMuted,
            onMutedChanged = onAudioMutedChanged,
            onVolumeChanged = onAudioVolumeChanged,
            onRetryFocus = onRetryAudioFocus,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = "Show audio dB labels" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Show level numbers",
                color = ndiMonitorColors().primaryText,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            CompactToggle(
                checked = showAudioDbLabels,
                onCheckedChange = onShowAudioDbLabelsChanged,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = ndiMonitorColors()
    Text(
        text = text,
        color = colors.secondaryText,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
    )
}

@Composable
private fun CompactToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = ndiMonitorColors()
    val trackShape = RoundedCornerShape(12.dp)
    Box(
        modifier =
            Modifier
                .size(width = 44.dp, height = 24.dp)
                .clip(trackShape)
                .background(if (checked) colors.accent.copy(alpha = 0.35f) else colors.sourceUpper)
                .border(1.dp, if (checked) colors.accent.copy(alpha = 0.75f) else colors.border, trackShape)
                .clickable { onCheckedChange(!checked) }
                .semantics {
                    contentDescription = "Show level numbers toggle"
                    role = Role.Switch
                },
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

private enum class PlaybackTab {
    VIDEO,
    AUDIO,
}

private val NdiBandwidth.description: String
    get() =
        when (this) {
            NdiBandwidth.AUTOMATIC -> "Choose quality automatically"
            NdiBandwidth.HIGHEST -> "Prefer full-quality stream"
            NdiBandwidth.LOWEST -> "Prefer lower-bandwidth preview"
        }
