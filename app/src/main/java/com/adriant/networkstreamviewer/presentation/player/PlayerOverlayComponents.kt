package com.adriant.networkstreamviewer.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.adriant.networkstreamviewer.ui.theme.ndiMonitorColors

@Composable
internal fun OverlayCloseButton(
    contentDescription: String,
    onClick: () -> Unit,
) {
    val colors = ndiMonitorColors()
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier =
            Modifier
                .size(36.dp)
                .clip(shape)
                .background(colors.sourceLower)
                .border(1.dp, colors.border, shape)
                .clickable(onClick = onClick)
                .semantics {
                    this.contentDescription = contentDescription
                    role = Role.Button
                },
        contentAlignment = Alignment.Center,
    ) {
        CloseIcon(Modifier.size(18.dp), color = colors.secondaryText)
    }
}

@Composable
internal fun PlayerOverlayButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = ndiMonitorColors()
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier =
            modifier
                .size(44.dp)
                .clip(shape)
                .background(colors.sourceUpper.copy(alpha = 0.94f))
                .border(1.dp, colors.border, shape)
                .clickable(onClick = onClick)
                .semantics {
                    this.contentDescription = contentDescription
                    role = Role.Button
                },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.primaryText) {
            content()
        }
    }
}
