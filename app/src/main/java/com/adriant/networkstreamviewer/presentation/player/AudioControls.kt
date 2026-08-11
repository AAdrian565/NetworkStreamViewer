package com.adriant.networkstreamviewer.presentation.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adriant.networkstreamviewer.domain.model.NdiAudioStatus
import com.adriant.networkstreamviewer.ui.theme.ndiMonitorColors
import java.util.Locale

@Composable
internal fun AudioControls(
    status: NdiAudioStatus,
    volume: Float,
    muted: Boolean,
    onMutedChanged: (Boolean) -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onRetryFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ndiMonitorColors()
    val currentOnVolumeChanged by rememberUpdatedState(onVolumeChanged)
    val normalizedVolume = volume.coerceIn(0f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (status == NdiAudioStatus.FOCUS_LOST) {
                                onRetryFocus()
                                onMutedChanged(false)
                            } else {
                                onMutedChanged(!muted)
                            }
                        }.semantics {
                            contentDescription = if (muted) "Unmute audio" else "Mute audio"
                        },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (muted) "🔇" else "🔊",
                    fontSize = 16.sp,
                )
            }
            CompactAudioSlider(
                value = normalizedVolume,
                onValueChange = currentOnVolumeChanged,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = String.format(Locale.US, "%d%%", (normalizedVolume * 100).toInt()),
                color = colors.secondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(32.dp),
            )
        }
        val message =
            when (status) {
                NdiAudioStatus.NO_SIGNAL -> "No audio"
                NdiAudioStatus.FOCUS_LOST -> "Audio focus lost — tap mute to retry"
                NdiAudioStatus.OUTPUT_FAILURE -> "Audio output failed"
                NdiAudioStatus.STARTING, NdiAudioStatus.PLAYING -> null
            }
        message?.let {
            Text(
                text = it,
                color = colors.secondaryText,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 39.dp, top = 4.dp),
            )
        }
    }
}

@Composable
private fun CompactAudioSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ndiMonitorColors()
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val sliderValue = value.coerceIn(0f, 1f)

    Canvas(
        modifier =
            modifier
                .height(32.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        currentOnValueChange((down.position.x / size.width).coerceIn(0f, 1f))
                        drag(down.id) { change ->
                            change.consume()
                            currentOnValueChange((change.position.x / size.width).coerceIn(0f, 1f))
                        }
                    }
                }.semantics {
                    contentDescription = "Audio volume"
                    progressBarRangeInfo = ProgressBarRangeInfo(sliderValue, 0f..1f)
                },
    ) {
        val trackInset = 6.dp.toPx()
        val trackY = size.height / 2f
        val trackStart = trackInset
        val trackEnd = size.width - trackInset
        val thumbX = trackStart + (trackEnd - trackStart) * sliderValue

        drawLine(
            color = colors.sourceUpper,
            start = Offset(trackStart, trackY),
            end = Offset(trackEnd, trackY),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = colors.accent,
            start = Offset(trackStart, trackY),
            end = Offset(thumbX, trackY),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = colors.accent,
            radius = 6.dp.toPx(),
            center = Offset(thumbX, trackY),
        )
        drawCircle(
            color = Color(0xFF06151A),
            radius = 2.dp.toPx(),
            center = Offset(thumbX, trackY),
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}
