package com.adriant.networkstreamviewer.presentation.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun PtzPresetControls(
    isSupported: Boolean,
    onRecallPreset: (Int) -> Unit,
    onStorePreset: (Int) -> Unit,
    onClearPreset: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isSupported) return

    var mode by remember { mutableStateOf(PresetMode.CALL) }
    var pendingStorePreset by remember { mutableStateOf<Int?>(null) }

    fun selectPreset(presetNumber: Int) {
        if (mode == PresetMode.SAVE) {
            pendingStorePreset = presetNumber
        } else {
            mode.perform(presetNumber, onRecallPreset, onStorePreset, onClearPreset)
            mode = PresetMode.CALL
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { contentDescription = "PTZ preset controls" },
    ) {
        Text(
            "STORED PRESETS",
            color = Color(0xFF00E5F0),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text =
                when (mode) {
                    PresetMode.CALL -> "Select a preset to call"
                    PresetMode.SAVE -> "Select a preset to save"
                    PresetMode.CLEAR -> "Select a preset to clear"
                },
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.58f),
        )
        Spacer(Modifier.size(10.dp))
        PRESET_ROWS.forEachIndexed { rowIndex, presets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { presetNumber ->
                    PresetButton(
                        presetNumber = presetNumber,
                        mode = mode,
                        onClick = { selectPreset(presetNumber) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (rowIndex < PRESET_ROWS.lastIndex) Spacer(Modifier.size(8.dp))
        }
        Spacer(Modifier.size(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModeButton(
                selected = mode == PresetMode.CLEAR,
                onSelectedChange = {
                    mode = if (it) PresetMode.CLEAR else PresetMode.CALL
                },
                contentDescription = "Clear preset mode",
                modifier = Modifier.weight(1f),
            ) {
                ClearIcon()
            }
            PresetButton(
                presetNumber = 0,
                mode = mode,
                onClick = { selectPreset(0) },
                modifier = Modifier.weight(1f),
            )
            ModeButton(
                selected = mode == PresetMode.SAVE,
                onSelectedChange = {
                    mode = if (it) PresetMode.SAVE else PresetMode.CALL
                },
                contentDescription = "Save preset mode",
                modifier = Modifier.weight(1f),
            ) {
                SaveIcon()
            }
        }
    }

    pendingStorePreset?.let { presetNumber ->
        AlertDialog(
            onDismissRequest = { pendingStorePreset = null },
            title = { Text("Overwrite preset $presetNumber?") },
            text = { Text("This replaces the camera position and focus stored in this preset.") },
            dismissButton = {
                TextButton(onClick = { pendingStorePreset = null }) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onStorePreset(presetNumber)
                        pendingStorePreset = null
                        mode = PresetMode.CALL
                    },
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Confirm overwrite preset $presetNumber"
                        },
                ) { Text("Overwrite") }
            },
        )
    }
}

@Composable
private fun PresetButton(
    presetNumber: Int,
    mode: PresetMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(42.dp)
                .background(Color(0xFF202126), RoundedCornerShape(7.dp))
                .clickable(onClick = onClick)
                .semantics {
                    contentDescription =
                        when (mode) {
                            PresetMode.CALL -> "Call preset $presetNumber"
                            PresetMode.SAVE -> "Save preset $presetNumber"
                            PresetMode.CLEAR -> "Clear preset $presetNumber"
                        }
                },
        contentAlignment = Alignment.Center,
    ) {
        Text(presetNumber.toString(), color = Color.White)
    }
}

@Composable
private fun ModeButton(
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .height(42.dp)
                .background(
                    if (selected) Color(0xFF075C65) else Color(0xFF202126),
                    RoundedCornerShape(7.dp),
                ).clickable { onSelectedChange(!selected) }
                .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

@Composable
private fun SaveIcon() {
    val color = LocalContentColor.current
    Canvas(modifier = Modifier.size(24.dp)) {
        val strokeWidth = 2.dp.toPx()
        drawRect(
            color = color,
            topLeft = Offset(size.width * 0.18f, size.height * 0.12f),
            size = Size(size.width * 0.64f, size.height * 0.76f),
            style = Stroke(width = strokeWidth),
        )
        drawRect(
            color = color,
            topLeft = Offset(size.width * 0.32f, size.height * 0.12f),
            size = Size(size.width * 0.36f, size.height * 0.26f),
            style = Stroke(width = strokeWidth),
        )
        drawRect(
            color = color,
            topLeft = Offset(size.width * 0.32f, size.height * 0.58f),
            size = Size(size.width * 0.36f, size.height * 0.30f),
            style = Stroke(width = strokeWidth),
        )
    }
}

@Composable
private fun ClearIcon() {
    val color = LocalContentColor.current
    Canvas(modifier = Modifier.size(24.dp)) {
        val strokeWidth = 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.28f, size.height * 0.20f),
            end = Offset(size.width * 0.72f, size.height * 0.80f),
            strokeWidth = strokeWidth,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.72f, size.height * 0.20f),
            end = Offset(size.width * 0.28f, size.height * 0.80f),
            strokeWidth = strokeWidth,
        )
    }
}

private enum class PresetMode {
    CALL,
    SAVE,
    CLEAR,
    ;

    fun perform(
        presetNumber: Int,
        onRecallPreset: (Int) -> Unit,
        onStorePreset: (Int) -> Unit,
        onClearPreset: (Int) -> Unit,
    ) = when (this) {
        CALL -> onRecallPreset(presetNumber)
        SAVE -> onStorePreset(presetNumber)
        CLEAR -> onClearPreset(presetNumber)
    }
}

private val PRESET_ROWS =
    listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
        listOf(7, 8, 9),
    )
