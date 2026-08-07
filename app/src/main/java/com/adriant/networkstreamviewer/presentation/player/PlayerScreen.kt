package com.adriant.networkstreamviewer.presentation.player

import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.adriant.networkstreamviewer.data.ndi.NdiPlayerController
import com.adriant.networkstreamviewer.domain.model.NdiBandwidth
import com.adriant.networkstreamviewer.domain.model.NdiPlaybackState
import com.adriant.networkstreamviewer.domain.model.NdiPtzCommandResult
import com.adriant.networkstreamviewer.domain.model.NdiSource
import com.adriant.networkstreamviewer.domain.model.isDeveloperExample
import kotlinx.coroutines.launch

private const val DEFAULT_VIDEO_ASPECT_RATIO = 16f / 9f

@Composable
fun PlayerScreen(
    source: NdiSource,
    playerController: NdiPlayerController,
    defaultBandwidth: NdiBandwidth,
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
    var bandwidth by remember(source) { mutableStateOf(defaultBandwidth) }
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

internal val NdiBandwidth.label: String
    get() = when (this) {
        NdiBandwidth.AUTOMATIC -> "Automatic"
        NdiBandwidth.HIGHEST -> "Highest"
        NdiBandwidth.LOWEST -> "Preview / Low"
    }

internal val NdiBandwidth.shortLabel: String
    get() = when (this) {
        NdiBandwidth.AUTOMATIC -> "Auto"
        NdiBandwidth.HIGHEST -> "High"
        NdiBandwidth.LOWEST -> "Low"
    }

internal val NdiPlaybackState.label: String
    get() = when (this) {
        NdiPlaybackState.CONNECTING -> "Connecting"
        NdiPlaybackState.WAITING_FOR_KEYFRAME -> "Waiting for keyframe"
        NdiPlaybackState.PLAYING -> "Playing"
        NdiPlaybackState.DISCONNECTED -> "Disconnected"
        NdiPlaybackState.UNSUPPORTED_CODEC -> "Unsupported codec"
        NdiPlaybackState.DECODER_FAILURE -> "Decoder failure"
        NdiPlaybackState.INSUFFICIENT_BANDWIDTH -> "Insufficient bandwidth"
    }

internal fun NdiPtzCommandResult.presetStatusMessage(action: String): String = when (this) {
    NdiPtzCommandResult.ACCEPTED -> "$action accepted."
    NdiPtzCommandResult.UNAVAILABLE -> "$action unavailable; the camera is not connected or PTZ-capable."
    NdiPtzCommandResult.REJECTED -> "$action was rejected by the camera."
    NdiPtzCommandResult.INVALID_ARGUMENT -> "$action has an invalid preset number or speed."
}
