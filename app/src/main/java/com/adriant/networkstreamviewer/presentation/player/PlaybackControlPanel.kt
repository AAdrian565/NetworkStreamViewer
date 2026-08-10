package com.adriant.networkstreamviewer.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adriant.networkstreamviewer.domain.model.NdiAudioStatus
import com.adriant.networkstreamviewer.domain.model.NdiBandwidth

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
    if (!isVisible) return
    var selectedTab by remember { mutableStateOf(PlaybackTab.VIDEO) }

    Column(
        modifier =
            modifier
                .width(320.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xE60B0D10), RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics { contentDescription = "Playback control panel" },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("▣", color = Color(0xFF00E5F0))
            Text(
                "PLAYBACK",
                color = Color(0xFF00E5F0),
                style = MaterialTheme.typography.labelMedium,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
            )
            SmallFloatingActionButton(
                onClick = onClose,
                modifier =
                    Modifier
                        .size(36.dp)
                        .semantics { contentDescription = "Hide playback controls" },
            ) {
                Text("AV", style = MaterialTheme.typography.labelSmall)
            }
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(Color(0xFF17191D), RoundedCornerShape(7.dp))
                    .padding(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            PlaybackTabButton(
                label = "Video",
                selected = selectedTab == PlaybackTab.VIDEO,
                onClick = { selectedTab = PlaybackTab.VIDEO },
                modifier = Modifier.weight(1f),
            )
            PlaybackTabButton(
                label = "Audio",
                selected = selectedTab == PlaybackTab.AUDIO,
                onClick = { selectedTab = PlaybackTab.AUDIO },
                modifier = Modifier.weight(1f),
            )
        }

        when (selectedTab) {
            PlaybackTab.VIDEO ->
                Column {
                    NdiBandwidth.entries.forEach { option ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onBandwidthChanged(option) }
                                    .semantics { contentDescription = "Video quality: ${option.label}" }
                                    .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = bandwidth == option,
                                onClick = { onBandwidthChanged(option) },
                            )
                            Text(option.label, color = Color.White)
                        }
                    }
                }
            PlaybackTab.AUDIO ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                .semantics { contentDescription = "Show audio dB labels" },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Show level numbers",
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = showAudioDbLabels,
                            onCheckedChange = onShowAudioDbLabelsChanged,
                        )
                    }
                }
        }
    }
}

@Composable
private fun PlaybackTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        color = if (selected) Color(0xFF00E5F0) else Color.White.copy(alpha = 0.68f),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelMedium,
        modifier =
            modifier
                .background(
                    if (selected) Color(0x3319E7F2) else Color.Transparent,
                    RoundedCornerShape(5.dp),
                ).clickable(onClick = onClick)
                .padding(vertical = 7.dp)
                .semantics { contentDescription = "$label playback tab" },
    )
}

private enum class PlaybackTab {
    VIDEO,
    AUDIO,
}
