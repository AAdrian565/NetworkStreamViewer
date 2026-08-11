package com.adriant.networkstreamviewer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Shared visual tokens for the AMOLED-first network monitor surfaces. */
data class NdiMonitorColors(
    val background: Color,
    val topBar: Color,
    val sourceUpper: Color,
    val sourceLower: Color,
    val border: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val metadataText: Color,
    val metadataLabel: Color,
    val accent: Color,
    val online: Color,
    val offline: Color,
    val countBadge: Color,
    val settingsButton: Color,
)

@Composable
fun ndiMonitorColors(): NdiMonitorColors {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() > 0.5f
    return NdiMonitorColors(
        background = scheme.background,
        topBar = scheme.surface,
        sourceUpper = scheme.surfaceVariant,
        sourceLower = scheme.surfaceContainerLow,
        border = scheme.outlineVariant,
        primaryText = scheme.onSurface,
        secondaryText = scheme.onSurfaceVariant,
        metadataText = scheme.onSurface,
        metadataLabel = scheme.onSurfaceVariant.copy(alpha = if (isLight) 0.72f else 0.78f),
        accent = scheme.primary,
        online = if (isLight) Color(0xFF16864A) else Color(0xFF45D17C),
        offline = if (isLight) Color(0xFFC63847) else Color(0xFFF15F6B),
        countBadge = if (isLight) Color(0xFFDCEAF7) else Color(0xFF172C4A),
        settingsButton = if (isLight) scheme.surfaceVariant else Color(0xFF181827),
    )
}
