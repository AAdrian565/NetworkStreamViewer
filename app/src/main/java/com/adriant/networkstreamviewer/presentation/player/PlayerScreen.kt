package com.adriant.networkstreamviewer.presentation.player

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.adriant.networkstreamviewer.data.ndi.NdiPlayerController
import com.adriant.networkstreamviewer.domain.model.NdiBandwidth
import com.adriant.networkstreamviewer.domain.model.NdiPlaybackState
import com.adriant.networkstreamviewer.domain.model.NdiPtzCommandResult
import com.adriant.networkstreamviewer.domain.model.NdiSource
import com.adriant.networkstreamviewer.domain.model.NdiVideoFormat
import com.adriant.networkstreamviewer.domain.model.isDeveloperExample
import kotlinx.coroutines.launch
import java.util.Locale

private const val DEFAULT_VIDEO_ASPECT_RATIO = 16f / 9f

@Composable
fun PlayerScreen(
    source: NdiSource,
    playerController: NdiPlayerController,
    keepScreenAwake: Boolean,
    showDiagnostics: Boolean,
    onBack: () -> Unit
) {
    ImmersiveSystemBarsEffect()
    KeepScreenAwakeEffect(enabled = keepScreenAwake)
    val isDeveloperExample = source.isDeveloperExample
    var videoAspectRatio by remember(source) {
        mutableFloatStateOf(DEFAULT_VIDEO_ASPECT_RATIO)
    }
    var bandwidth by remember(source) { mutableStateOf(NdiBandwidth.AUTOMATIC) }
    var automaticFallbackToLow by remember(source, bandwidth) { mutableStateOf(false) }
    var playbackState by remember(source, bandwidth) {
        mutableStateOf(
            if (isDeveloperExample) NdiPlaybackState.PLAYING else NdiPlaybackState.CONNECTING
        )
    }
    var retryGeneration by remember(source, bandwidth) { mutableIntStateOf(0) }
    var bandwidthMenuExpanded by remember { mutableStateOf(false) }
    var isPtzSupported by remember(source, bandwidth) { mutableStateOf(isDeveloperExample) }
    var isPtzOverlayVisible by remember(source, bandwidth) { mutableStateOf(false) }
    var ptzStatusMessage by remember(source) { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
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
            if (isDeveloperExample) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF101010))
                )
            } else {
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
                                    },
                                    onPtzSupportChanged = { isSupported ->
                                        post {
                                            if (isPtzSupported && !isSupported) {
                                                ptzStatusMessage = "PTZ controls are no longer available."
                                            }
                                            isPtzSupported = isSupported
                                            if (!isSupported) isPtzOverlayVisible = false
                                        }
                                    }
                                )
                            )
                        }
                    }
                )
            }
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
            Column(horizontalAlignment = Alignment.End) {
                Box {
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
                if (isPtzSupported) {
                    Spacer(Modifier.size(8.dp))
                    SmallFloatingActionButton(
                        onClick = { isPtzOverlayVisible = !isPtzOverlayVisible },
                        modifier = Modifier.semantics {
                            contentDescription = if (isPtzOverlayVisible) {
                                "Hide PTZ controls"
                            } else {
                                "Show PTZ controls"
                            }
                        }
                    ) {
                        Text("PTZ")
                    }
                }
            }
        }

        if (showDiagnostics) {
            PlaybackDiagnostics(
                source = source,
                bandwidth = bandwidth,
                automaticFallbackToLow = automaticFallbackToLow,
                playbackState = playbackState,
                isDeveloperExample = isDeveloperExample,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        }

        PtzPresetControls(
            isSupported = isPtzSupported && isPtzOverlayVisible,
            onRecallPreset = { presetNumber ->
                if (isDeveloperExample) {
                    ptzStatusMessage = "Demo: preset $presetNumber recall accepted."
                } else coroutineScope.launch {
                    ptzStatusMessage = playerController
                        .recallPtzPreset(presetNumber)
                        .presetStatusMessage("Preset $presetNumber recall")
                }
            },
            onStorePreset = { presetNumber ->
                if (isDeveloperExample) {
                    ptzStatusMessage = "Demo: preset $presetNumber store accepted."
                } else coroutineScope.launch {
                    ptzStatusMessage = playerController
                        .storePtzPreset(presetNumber)
                        .presetStatusMessage("Preset $presetNumber store")
                }
            },
            onClearPreset = { presetNumber ->
                ptzStatusMessage = if (isDeveloperExample) {
                    "Demo: preset $presetNumber cleared."
                } else {
                    "Preset clearing is not supported by the NDI® PTZ API."
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 88.dp)
        )

        ptzStatusMessage?.let { message ->
            Surface(
                color = Color.Black.copy(alpha = 0.84f),
                contentColor = Color.White,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .semantics { contentDescription = "PTZ command status" }
            ) {
                Text(message, modifier = Modifier.padding(12.dp))
            }
        }
    }

    DisposableEffect(source) {
        onDispose {
            if (!isDeveloperExample) playerController.stop()
        }
    }
}

@Composable
private fun KeepScreenAwakeEffect(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        val previousValue = view.keepScreenOn
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = previousValue }
    }
}

@Composable
private fun PlaybackDiagnostics(
    source: NdiSource,
    bandwidth: NdiBandwidth,
    automaticFallbackToLow: Boolean,
    playbackState: NdiPlaybackState,
    isDeveloperExample: Boolean,
    modifier: Modifier = Modifier
) {
    val details = source.details
    val codec = when (details?.format) {
        NdiVideoFormat.FULL_NDI -> "Full NDI"
        NdiVideoFormat.HX_H264 -> "H.264"
        NdiVideoFormat.HX_HEVC -> "HEVC"
        null -> "Unknown"
    }
    val resolution = details?.let { "${it.width}×${it.height}" } ?: "Unknown"
    val framesPerSecond = details?.let {
        if (it.frameRateDenominator > 0) {
            String.format(Locale.US, "%.2f", it.frameRateNumerator.toDouble() / it.frameRateDenominator)
                .removeSuffix(".00")
        } else {
            "Unknown"
        }
    } ?: "Unknown"
    val bandwidthText = if (bandwidth == NdiBandwidth.AUTOMATIC && automaticFallbackToLow) {
        "Automatic → Preview / Low"
    } else {
        bandwidth.label
    }
    val connectionText = if (isDeveloperExample) "Playing (simulated)" else playbackState.label

    Surface(
        color = Color.Black.copy(alpha = 0.78f),
        contentColor = Color.White,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = modifier.semantics { contentDescription = "Playback diagnostics" }
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text("Codec: $codec • $resolution • $framesPerSecond FPS")
            Text("Bandwidth: $bandwidthText • State: $connectionText")
        }
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

private val NdiPlaybackState.label: String
    get() = when (this) {
        NdiPlaybackState.CONNECTING -> "Connecting"
        NdiPlaybackState.WAITING_FOR_KEYFRAME -> "Waiting for keyframe"
        NdiPlaybackState.PLAYING -> "Playing"
        NdiPlaybackState.DISCONNECTED -> "Disconnected"
        NdiPlaybackState.UNSUPPORTED_CODEC -> "Unsupported codec"
        NdiPlaybackState.DECODER_FAILURE -> "Decoder failure"
        NdiPlaybackState.INSUFFICIENT_BANDWIDTH -> "Insufficient bandwidth"
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
    onPlaybackStateChanged: (NdiPlaybackState) -> Unit,
    onPtzSupportChanged: (Boolean) -> Unit
) = object : SurfaceHolder.Callback {
    override fun surfaceCreated(holder: SurfaceHolder) {
        onPlaybackStateChanged(NdiPlaybackState.CONNECTING)
        onPtzSupportChanged(false)
        if (!playerController.start(
                source = source,
                surface = holder.surface,
                bandwidth = bandwidth,
                onAspectRatioChanged = onAspectRatioChanged,
                onPlaybackStateChanged = onPlaybackStateChanged,
                onPtzSupportChanged = onPtzSupportChanged
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
        onPtzSupportChanged(false)
        playerController.stop()
    }
}

internal fun NdiPtzCommandResult.presetStatusMessage(action: String): String = when (this) {
    NdiPtzCommandResult.ACCEPTED -> "$action accepted."
    NdiPtzCommandResult.UNAVAILABLE -> "$action unavailable; the camera is not connected or PTZ-capable."
    NdiPtzCommandResult.REJECTED -> "$action was rejected by the camera."
    NdiPtzCommandResult.INVALID_ARGUMENT -> "$action has an invalid preset number or speed."
}
