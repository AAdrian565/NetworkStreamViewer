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
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.unit.dp

@Composable
internal fun PtzControlPanel(
    isSupported: Boolean,
    onPanTiltSpeed: (Float, Float) -> Unit,
    onZoomSpeed: (Float) -> Unit,
    onFocusSpeed: (Float) -> Unit,
    onStop: () -> Unit,
    onAutoFocus: () -> Unit,
    onToggleVisibility: () -> Unit,
    onRecallPreset: (Int) -> Unit,
    onStorePreset: (Int) -> Unit,
    onClearPreset: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isSupported) return

    var selectedTab by remember { mutableStateOf(PtzTab.PRESETS) }

    Column(
        modifier =
            modifier
                .width(320.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xE60B0D10), RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics { contentDescription = "PTZ control panel" },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("▣", color = Color(0xFF00E5F0))
            Text(
                "PTZ ACTIVE",
                color = Color(0xFF00E5F0),
                style = MaterialTheme.typography.labelMedium,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
            )
            SmallFloatingActionButton(
                onClick = onToggleVisibility,
                modifier =
                    Modifier
                        .size(36.dp)
                        .semantics { contentDescription = "Hide PTZ controls" },
            ) {
                PtzControlsIcon(Modifier.size(20.dp))
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
            PtzTabButton(
                label = "Presets",
                selected = selectedTab == PtzTab.PRESETS,
                onClick = { selectedTab = PtzTab.PRESETS },
                modifier = Modifier.weight(1f),
            )
            PtzTabButton(
                label = "Manual",
                selected = selectedTab == PtzTab.MANUAL,
                onClick = { selectedTab = PtzTab.MANUAL },
                modifier = Modifier.weight(1f),
            )
        }

        when (selectedTab) {
            PtzTab.MANUAL ->
                PtzCameraControls(
                    isSupported = true,
                    onPanTiltSpeed = onPanTiltSpeed,
                    onZoomSpeed = onZoomSpeed,
                    onFocusSpeed = onFocusSpeed,
                    onStop = onStop,
                    onAutoFocus = onAutoFocus,
                )
            PtzTab.PRESETS ->
                PtzPresetControls(
                    isSupported = true,
                    onRecallPreset = onRecallPreset,
                    onStorePreset = onStorePreset,
                    onClearPreset = onClearPreset,
                )
        }
    }
}

@Composable
private fun PtzTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        color = if (selected) Color(0xFF00E5F0) else Color.White.copy(alpha = 0.68f),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        style = MaterialTheme.typography.labelMedium,
        modifier =
            modifier
                .background(
                    if (selected) Color(0x3319E7F2) else Color.Transparent,
                    RoundedCornerShape(5.dp),
                ).clickable(onClick = onClick)
                .padding(vertical = 7.dp)
                .semantics { contentDescription = "$label PTZ tab" },
    )
}

private enum class PtzTab {
    PRESETS,
    MANUAL,
}
