package com.adriant.networkstreamviewer.presentation.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.adriant.networkstreamviewer.domain.model.NdiAudioLevels
import com.adriant.networkstreamviewer.domain.model.NdiAudioStatus
import java.util.Locale

@Composable
internal fun AudioMeter(
    levels: NdiAudioLevels,
    status: NdiAudioStatus,
    showDbLabels: Boolean = true,
    modifier: Modifier = Modifier
) {
    val description = String.format(
        Locale.US,
        "Audio meter: left %.0f dB, right %.0f dB",
        levels.leftRmsDbfs,
        levels.rightRmsDbfs
    )
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = description
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MeterBar(
            rms = levels.leftRmsDbfs,
            peak = levels.leftPeakDbfs,
            clipped = levels.leftClipped,
            active = status == NdiAudioStatus.PLAYING,
            showDbLabel = showDbLabels
        )
        MeterBar(
            rms = levels.rightRmsDbfs,
            peak = levels.rightPeakDbfs,
            clipped = levels.rightClipped,
            active = status == NdiAudioStatus.PLAYING,
            showDbLabel = showDbLabels
        )
    }
}

@Composable
private fun MeterBar(
    rms: Float,
    peak: Float,
    clipped: Boolean,
    active: Boolean,
    showDbLabel: Boolean
) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        if (showDbLabel) {
            Text(
                text = String.format(Locale.US, "%.0f", rms),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
        Canvas(modifier = Modifier.size(width = 14.dp, height = 104.dp)) {
            val floor = 60f
            fun y(db: Float): Float = size.height * (1f - ((db.coerceIn(-60f, 0f) + floor) / floor))
            val trackColor = if (active) Color.DarkGray else Color(0xFF3A3A3A)
            drawRoundRect(trackColor, cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f))
            val rmsY = y(rms)
            val fillTopLeft = androidx.compose.ui.geometry.Offset(0f, rmsY)
            val fillSize = androidx.compose.ui.geometry.Size(size.width, size.height - rmsY)
            if (active) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0f to Color(0xFFFF3B30),
                        0.05f to Color(0xFFFF3B30),
                        0.20f to Color(0xFFFFB300),
                        0.22f to Color(0xFF42D66B),
                        1f to Color(0xFF42D66B),
                        startY = 0f,
                        endY = size.height
                    ),
                    topLeft = fillTopLeft,
                    size = fillSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                )
            } else {
                drawRoundRect(
                    color = Color.Gray,
                    topLeft = fillTopLeft,
                    size = fillSize,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
                )
            }
            val peakY = y(peak)
            drawRect(Color.White, androidx.compose.ui.geometry.Offset(0f, peakY), androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx()))
            if (clipped) drawRect(Color.Red, size = androidx.compose.ui.geometry.Size(size.width, 4.dp.toPx()))
        }
    }
}
