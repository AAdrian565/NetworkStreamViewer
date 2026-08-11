package com.adriant.networkstreamviewer.presentation.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adriant.networkstreamviewer.domain.model.NdiAudioLevels
import com.adriant.networkstreamviewer.domain.model.NdiAudioStatus
import com.adriant.networkstreamviewer.ui.theme.ndiMonitorColors
import java.util.Locale

@Composable
internal fun AudioMeter(
    levels: NdiAudioLevels,
    status: NdiAudioStatus,
    modifier: Modifier = Modifier,
    showDbLabels: Boolean = true,
) {
    val description =
        String.format(
            Locale.US,
            "Audio meter: left %.0f dB, right %.0f dB",
            levels.leftRmsDbfs,
            levels.rightRmsDbfs,
        )
    Column(
        modifier =
            modifier
                .semantics(mergeDescendants = true) { contentDescription = description }
                .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MeterBar(
                channel = "L",
                rms = levels.leftRmsDbfs,
                peak = levels.leftPeakDbfs,
                clipped = levels.leftClipped,
                active = status == NdiAudioStatus.PLAYING,
                showDbLabel = showDbLabels,
            )
            MeterBar(
                channel = "R",
                rms = levels.rightRmsDbfs,
                peak = levels.rightPeakDbfs,
                clipped = levels.rightClipped,
                active = status == NdiAudioStatus.PLAYING,
                showDbLabel = showDbLabels,
            )
        }
    }
}

@Composable
private fun MeterBar(
    channel: String,
    rms: Float,
    peak: Float,
    clipped: Boolean,
    active: Boolean,
    showDbLabel: Boolean,
) {
    val colors = ndiMonitorColors()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showDbLabel) {
            Text(
                text = String.format(Locale.US, "%.0f", rms),
                color = colors.primaryText,
                fontSize = 10.sp,
                lineHeight = 11.sp,
            )
        }
        Text(
            text = channel,
            color = colors.secondaryText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
        Canvas(modifier = Modifier.size(width = 14.dp, height = 88.dp)) {
            val floor = 60f

            fun y(db: Float): Float = size.height * (1f - ((db.coerceIn(-60f, 0f) + floor) / floor))

            val trackColor = if (active) Color(0xFF1C2B30) else Color(0xFF24242E)
            drawRoundRect(
                color = trackColor,
                cornerRadius =
                    androidx.compose.ui.geometry
                        .CornerRadius(5f, 5f),
            )
            val rmsY = y(rms)
            val fillTopLeft =
                androidx.compose.ui.geometry
                    .Offset(0f, rmsY)
            val fillSize =
                androidx.compose.ui.geometry
                    .Size(size.width, size.height - rmsY)
            if (active) {
                drawRoundRect(
                    brush =
                        Brush.verticalGradient(
                            0f to Color(0xFFFF3B30),
                            0.05f to Color(0xFFFF3B30),
                            0.20f to Color(0xFFFFB300),
                            0.22f to Color(0xFF35D46B),
                            1f to Color(0xFF35D46B),
                            startY = 0f,
                            endY = size.height,
                        ),
                    topLeft = fillTopLeft,
                    size = fillSize,
                    cornerRadius =
                        androidx.compose.ui.geometry
                            .CornerRadius(5f, 5f),
                )
            } else {
                drawRoundRect(
                    color = Color(0xFF5E6570),
                    topLeft = fillTopLeft,
                    size = fillSize,
                    cornerRadius =
                        androidx.compose.ui.geometry
                            .CornerRadius(5f, 5f),
                )
            }
            val peakY = y(peak)
            drawRect(
                color = Color.White.copy(alpha = 0.8f),
                topLeft =
                    androidx.compose.ui.geometry
                        .Offset(0f, peakY),
                size =
                    androidx.compose.ui.geometry
                        .Size(size.width, 2.dp.toPx()),
            )
            if (clipped) {
                drawRect(
                    color = Color(0xFFFF5D67),
                    size =
                        androidx.compose.ui.geometry
                            .Size(size.width, 4.dp.toPx()),
                )
            }
        }
    }
}
