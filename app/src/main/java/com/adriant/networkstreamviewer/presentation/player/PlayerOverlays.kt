package com.adriant.networkstreamviewer.presentation.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adriant.networkstreamviewer.domain.model.NdiAudioDiagnostics
import com.adriant.networkstreamviewer.domain.model.NdiAudioStatus
import com.adriant.networkstreamviewer.domain.model.NdiBandwidth
import com.adriant.networkstreamviewer.domain.model.NdiPlaybackState
import com.adriant.networkstreamviewer.domain.model.NdiSource
import com.adriant.networkstreamviewer.domain.model.NdiVideoDiagnostics
import com.adriant.networkstreamviewer.domain.model.NdiVideoFormat
import com.adriant.networkstreamviewer.ui.theme.ndiMonitorColors
import java.util.Locale

@Composable
internal fun KeepScreenAwakeEffect(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        val previousValue = view.keepScreenOn
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = previousValue }
    }
}

@Composable
internal fun PlaybackDiagnostics(
    source: NdiSource,
    bandwidth: NdiBandwidth,
    automaticFallbackToLow: Boolean,
    playbackState: NdiPlaybackState,
    isDeveloperExample: Boolean,
    modifier: Modifier = Modifier,
    videoDiagnostics: NdiVideoDiagnostics = NdiVideoDiagnostics(),
    audioDiagnostics: NdiAudioDiagnostics = NdiAudioDiagnostics(),
) {
    val details = source.details
    val codec =
        when (details?.format) {
            NdiVideoFormat.FULL_NDI -> "Full NDI"
            NdiVideoFormat.HX_H264 -> "H.264"
            NdiVideoFormat.HX_HEVC -> "HEVC"
            null -> "Unknown"
        }
    val resolution =
        if (videoDiagnostics.width > 0 && videoDiagnostics.height > 0) {
            "${videoDiagnostics.width}×${videoDiagnostics.height}"
        } else {
            details?.let { "${it.width}×${it.height}" } ?: "Unknown"
        }
    val framesPerSecond =
        details?.let {
            if (it.frameRateDenominator > 0) {
                String
                    .format(Locale.US, "%.2f", it.frameRateNumerator.toDouble() / it.frameRateDenominator)
                    .removeSuffix(".00")
            } else {
                "Unknown"
            }
        } ?: "Unknown"
    val bandwidthText =
        if (bandwidth == NdiBandwidth.AUTOMATIC && automaticFallbackToLow) {
            "Automatic → Preview / Low"
        } else {
            bandwidth.label
        }
    val connectionText = if (isDeveloperExample) "Playing (simulated)" else playbackState.label

    val colors = ndiMonitorColors()
    val audioText =
        when (audioDiagnostics.status) {
            NdiAudioStatus.STARTING -> "Starting"
            NdiAudioStatus.PLAYING -> "Playing"
            NdiAudioStatus.NO_SIGNAL -> "No signal"
            NdiAudioStatus.FOCUS_LOST -> "Focus lost"
            NdiAudioStatus.OUTPUT_FAILURE -> "Output failure"
        }
    Column(
        modifier =
            modifier
                .semantics { contentDescription = "Playback diagnostics" }
                .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Text(
            text = "Codec: $codec • $resolution • $framesPerSecond FPS",
            color = colors.primaryText,
            fontSize = 11.sp,
            lineHeight = 12.sp,
        )
        Text(
            text = "Bandwidth: $bandwidthText • State: $connectionText",
            color = colors.secondaryText,
            fontSize = 11.sp,
            lineHeight = 12.sp,
        )
        Text(
            text =
                "Video: ${formatDiagnosticRate(videoDiagnostics.receivedFps)} in • " +
                    "${formatDiagnosticRate(videoDiagnostics.renderedFps)} out • " +
                    "Dropped: ${videoDiagnostics.droppedFrames}",
            color = colors.secondaryText,
            fontSize = 10.sp,
            lineHeight = 11.sp,
        )
        Text(
            text =
                "Queue: ${videoDiagnostics.queueDepth} • " +
                    "Process: ${formatDiagnosticRate(videoDiagnostics.processingTimeMs)} ms",
            color = colors.secondaryText,
            fontSize = 10.sp,
            lineHeight = 11.sp,
        )
        Text(
            text =
                "Audio: ${audioDiagnostics.outputSampleRate / 1000} kHz " +
                    "${if (audioDiagnostics.outputChannelCount == 2) "stereo" else "mono"} • " +
                    "$audioText • Dropped: ${audioDiagnostics.droppedFrames} • " +
                    "Underruns: ${audioDiagnostics.underrunCount}",
            color = colors.secondaryText,
            fontSize = 10.sp,
            lineHeight = 11.sp,
        )
    }
}

private fun formatDiagnosticRate(value: Float): String = String.format(Locale.US, "%.1f", value.coerceAtLeast(0f))

@Composable
internal fun PlaybackStatePanel(
    state: NdiPlaybackState,
    onRetry: () -> Unit,
) {
    val isProgress =
        state == NdiPlaybackState.CONNECTING ||
            state == NdiPlaybackState.WAITING_FOR_KEYFRAME
    val title =
        when (state) {
            NdiPlaybackState.CONNECTING -> "Connecting…"
            NdiPlaybackState.WAITING_FOR_KEYFRAME -> "Waiting for video…"
            NdiPlaybackState.PLAYING -> return
            NdiPlaybackState.DISCONNECTED -> "Source disconnected"
            NdiPlaybackState.UNSUPPORTED_CODEC -> "Unsupported video format"
            NdiPlaybackState.DECODER_FAILURE -> "Video decoder failed"
            NdiPlaybackState.INSUFFICIENT_BANDWIDTH -> "Network bandwidth is insufficient"
        }
    val detail =
        when (state) {
            NdiPlaybackState.CONNECTING -> "Opening the NDI stream."
            NdiPlaybackState.WAITING_FOR_KEYFRAME -> "Waiting for the next decodable keyframe."
            NdiPlaybackState.DISCONNECTED -> "The source is offline or no longer reachable."
            NdiPlaybackState.UNSUPPORTED_CODEC -> "This stream uses a format the app cannot display."
            NdiPlaybackState.DECODER_FAILURE -> "Android could not start or continue the hardware decoder."
            NdiPlaybackState.INSUFFICIENT_BANDWIDTH -> "Try Preview/Low or move to a faster network."
            NdiPlaybackState.PLAYING -> ""
        }

    val colors = ndiMonitorColors()
    val shape = RoundedCornerShape(16.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .widthIn(max = 380.dp)
                .padding(24.dp)
                .clip(shape)
                .background(colors.sourceUpper.copy(alpha = 0.94f))
                .border(1.dp, colors.border, shape)
                .padding(24.dp),
    ) {
        if (isProgress) {
            CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(32.dp))
            Spacer(Modifier.size(16.dp))
        }
        Text(title, color = colors.primaryText, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.size(8.dp))
        Text(detail, color = colors.secondaryText)
        if (!isProgress) {
            Spacer(Modifier.size(16.dp))
            Button(
                onClick = onRetry,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = Color(0xFF003640),
                    ),
            ) {
                Text("Retry", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun CenteredBackArrow() {
    val color = LocalContentColor.current
    Canvas(modifier = Modifier.size(24.dp)) {
        val arrow =
            Path().apply {
                moveTo(size.width * 0.70f, size.height * 0.20f)
                lineTo(size.width * 0.30f, size.height * 0.50f)
                lineTo(size.width * 0.70f, size.height * 0.80f)
            }
        drawPath(
            path = arrow,
            color = color,
            style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
