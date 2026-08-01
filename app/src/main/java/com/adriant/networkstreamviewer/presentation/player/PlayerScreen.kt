package com.adriant.networkstreamviewer.presentation.player

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.adriant.networkstreamviewer.data.ndi.NdiPlayerController
import com.adriant.networkstreamviewer.domain.model.NdiBandwidth
import com.adriant.networkstreamviewer.domain.model.NdiPlaybackState
import com.adriant.networkstreamviewer.domain.model.NdiSource

private const val DEFAULT_VIDEO_ASPECT_RATIO = 16f / 9f

@Composable
fun PlayerScreen(
    source: NdiSource,
    playerController: NdiPlayerController,
    onBack: () -> Unit
) {
    ImmersiveSystemBarsEffect()
    var videoAspectRatio by remember(source) {
        mutableFloatStateOf(DEFAULT_VIDEO_ASPECT_RATIO)
    }
    var bandwidth by remember(source) { mutableStateOf(NdiBandwidth.AUTOMATIC) }
    var automaticFallbackToLow by remember(source, bandwidth) { mutableStateOf(false) }
    var playbackState by remember(source, bandwidth) {
        mutableStateOf(NdiPlaybackState.CONNECTING)
    }
    var retryGeneration by remember(source, bandwidth) { mutableIntStateOf(0) }
    var bandwidthMenuExpanded by remember { mutableStateOf(false) }
    val receiverBandwidth = if (bandwidth == NdiBandwidth.AUTOMATIC && automaticFallbackToLow) {
        NdiBandwidth.LOWEST
    } else {
        bandwidth
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        key(source.name, source.url, receiverBandwidth, retryGeneration) {
            AndroidView(
                modifier = Modifier.aspectRatio(videoAspectRatio),
                factory = { context ->
                    SurfaceView(context).apply {
                        holder.addCallback(
                            playerSurfaceCallback(
                                source = source,
                                bandwidth = receiverBandwidth,
                                playerController = playerController,
                                onAspectRatioChanged = { ratio ->
                                    post { videoAspectRatio = ratio }
                                },
                                onPlaybackStateChanged = { state ->
                                    post {
                                        if (state == NdiPlaybackState.INSUFFICIENT_BANDWIDTH &&
                                            bandwidth == NdiBandwidth.AUTOMATIC &&
                                            !automaticFallbackToLow
                                        ) {
                                            automaticFallbackToLow = true
                                            playbackState = NdiPlaybackState.CONNECTING
                                        } else {
                                            playbackState = state
                                        }
                                    }
                                }
                            )
                        )
                    }
                }
            )
        }

        if (playbackState != NdiPlaybackState.PLAYING) {
            PlaybackStatePanel(
                state = playbackState,
                onRetry = { retryGeneration++ }
            )
        }

        SmallFloatingActionButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .semantics { contentDescription = "Back to source list" }
        ) {
            CenteredBackArrow()
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { bandwidthMenuExpanded = true },
                modifier = Modifier.semantics {
                    contentDescription = "Playback bandwidth: ${bandwidth.label}"
                }
            ) {
                Text(bandwidth.shortLabel)
            }
            DropdownMenu(
                expanded = bandwidthMenuExpanded,
                onDismissRequest = { bandwidthMenuExpanded = false }
            ) {
                NdiBandwidth.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            bandwidthMenuExpanded = false
                            bandwidth = option
                            automaticFallbackToLow = false
                        }
                    )
                }
            }
        }
    }

    DisposableEffect(source) {
        onDispose { playerController.stop() }
    }
}

@Composable
private fun PlaybackStatePanel(
    state: NdiPlaybackState,
    onRetry: () -> Unit
) {
    val isProgress = state == NdiPlaybackState.CONNECTING ||
        state == NdiPlaybackState.WAITING_FOR_KEYFRAME
    val title = when (state) {
        NdiPlaybackState.CONNECTING -> "Connecting…"
        NdiPlaybackState.WAITING_FOR_KEYFRAME -> "Waiting for video…"
        NdiPlaybackState.PLAYING -> return
        NdiPlaybackState.DISCONNECTED -> "Source disconnected"
        NdiPlaybackState.UNSUPPORTED_CODEC -> "Unsupported video format"
        NdiPlaybackState.DECODER_FAILURE -> "Video decoder failed"
        NdiPlaybackState.INSUFFICIENT_BANDWIDTH -> "Network bandwidth is insufficient"
    }
    val detail = when (state) {
        NdiPlaybackState.CONNECTING -> "Opening the NDI stream."
        NdiPlaybackState.WAITING_FOR_KEYFRAME -> "Waiting for the next decodable keyframe."
        NdiPlaybackState.DISCONNECTED -> "The source is offline or no longer reachable."
        NdiPlaybackState.UNSUPPORTED_CODEC -> "This stream uses a format the app cannot display."
        NdiPlaybackState.DECODER_FAILURE -> "Android could not start or continue the hardware decoder."
        NdiPlaybackState.INSUFFICIENT_BANDWIDTH -> "Try Preview/Low or move to a faster network."
        NdiPlaybackState.PLAYING -> ""
    }

    Surface(
        color = Color.Black.copy(alpha = 0.78f),
        contentColor = Color.White,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        modifier = Modifier
            .widthIn(max = 380.dp)
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            if (isProgress) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))
            }
            Text(title)
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            Text(detail, color = Color.White.copy(alpha = 0.8f))
            if (!isProgress) {
                androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

private val NdiBandwidth.label: String
    get() = when (this) {
        NdiBandwidth.AUTOMATIC -> "Automatic"
        NdiBandwidth.HIGHEST -> "Highest"
        NdiBandwidth.LOWEST -> "Preview / Low"
    }

private val NdiBandwidth.shortLabel: String
    get() = when (this) {
        NdiBandwidth.AUTOMATIC -> "Auto"
        NdiBandwidth.HIGHEST -> "High"
        NdiBandwidth.LOWEST -> "Low"
    }

@Composable
private fun CenteredBackArrow() {
    val color = LocalContentColor.current
    Canvas(modifier = Modifier.size(24.dp)) {
        val arrow = Path().apply {
            moveTo(size.width * 0.70f, size.height * 0.20f)
            lineTo(size.width * 0.30f, size.height * 0.50f)
            lineTo(size.width * 0.70f, size.height * 0.80f)
        }
        drawPath(
            path = arrow,
            color = color,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

private fun playerSurfaceCallback(
    source: NdiSource,
    bandwidth: NdiBandwidth,
    playerController: NdiPlayerController,
    onAspectRatioChanged: (Float) -> Unit,
    onPlaybackStateChanged: (NdiPlaybackState) -> Unit
) = object : SurfaceHolder.Callback {
    override fun surfaceCreated(holder: SurfaceHolder) {
        onPlaybackStateChanged(NdiPlaybackState.CONNECTING)
        if (!playerController.start(
                source = source,
                surface = holder.surface,
                bandwidth = bandwidth,
                onAspectRatioChanged = onAspectRatioChanged,
                onPlaybackStateChanged = onPlaybackStateChanged
            )) {
            onPlaybackStateChanged(NdiPlaybackState.DECODER_FAILURE)
        }
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        playerController.stop()
    }
}
