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
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.adriant.networkstreamviewer.data.ndi.NdiPlayerController
import com.adriant.networkstreamviewer.domain.model.NdiAudioDiagnostics
import com.adriant.networkstreamviewer.domain.model.NdiAudioLevels
import com.adriant.networkstreamviewer.domain.model.NdiAudioStatus
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
    onBack: () -> Unit,
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
            if (isDeveloperExample) NdiPlaybackState.PLAYING else NdiPlaybackState.CONNECTING,
        )
    }
    var retryGeneration by remember(source, bandwidth) { mutableIntStateOf(0) }
    var isPtzSupported by remember(source, bandwidth) { mutableStateOf(isDeveloperExample) }
    var isPtzOverlayVisible by remember(source, bandwidth) { mutableStateOf(false) }
    var isPlaybackOverlayVisible by remember(source) { mutableStateOf(false) }
    var ptzStatusMessage by remember(source) { mutableStateOf<String?>(null) }
    var audioVolume by rememberSaveable(source.name, source.url) { mutableFloatStateOf(1f) }
    var audioMuted by rememberSaveable(source.name, source.url) { mutableStateOf(false) }
    var showAudioDbLabels by rememberSaveable(source.name, source.url) { mutableStateOf(true) }
    var audioStatus by remember(source, bandwidth) {
        mutableStateOf(if (isDeveloperExample) NdiAudioStatus.PLAYING else NdiAudioStatus.STARTING)
    }
    var audioLevels by remember(source, bandwidth) {
        mutableStateOf(if (isDeveloperExample) NdiAudioLevels(-12f, -14f, -18f, -20f) else NdiAudioLevels.FLOOR)
    }
    var audioDiagnostics by remember(source, bandwidth) {
        mutableStateOf(
            NdiAudioDiagnostics(status = if (isDeveloperExample) NdiAudioStatus.PLAYING else NdiAudioStatus.STARTING),
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val receiverBandwidth =
        if (bandwidth == NdiBandwidth.AUTOMATIC && automaticFallbackToLow) {
            NdiBandwidth.LOWEST
        } else {
            bandwidth
        }

    fun runPtzCommand(
        action: String,
        command: suspend () -> NdiPtzCommandResult,
    ) {
        if (isDeveloperExample) {
            ptzStatusMessage = "Demo: $action accepted."
        } else {
            coroutineScope.launch {
                val result = command()
                if (result != NdiPtzCommandResult.ACCEPTED) {
                    ptzStatusMessage = result.ptzStatusMessage(action)
                }
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        key(source.name, source.url, receiverBandwidth, retryGeneration) {
            if (isDeveloperExample) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xFF101010)),
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
                                    initialAudioVolume = audioVolume,
                                    initialAudioMuted = audioMuted,
                                    onAudioStatusChanged = { value ->
                                        post { audioStatus = value }
                                    },
                                    onAudioLevelsChanged = { value ->
                                        post { audioLevels = value }
                                    },
                                    onAudioDiagnosticsChanged = { value ->
                                        post { audioDiagnostics = value }
                                    },
                                    onPtzSupportChanged = { isSupported ->
                                        post {
                                            if (isPtzSupported && !isSupported) {
                                                ptzStatusMessage = "PTZ controls are no longer available."
                                            }
                                            isPtzSupported = isSupported
                                            if (!isSupported) isPtzOverlayVisible = false
                                        }
                                    },
                                ),
                            )
                        }
                    },
                )
            }
        }

        if (playbackState != NdiPlaybackState.PLAYING) {
            PlaybackStatePanel(
                state = playbackState,
                onRetry = { retryGeneration++ },
            )
        }

        SmallFloatingActionButton(
            onClick = onBack,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .semantics { contentDescription = "Back to source list" },
        ) {
            CenteredBackArrow()
        }

        if (!isPtzOverlayVisible && !isPlaybackOverlayVisible) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
            ) {
                SmallFloatingActionButton(
                    onClick = { isPlaybackOverlayVisible = true },
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Show playback controls"
                        },
                ) {
                    PlaybackControlsIcon(Modifier.size(24.dp))
                }
                if (isPtzSupported) {
                    Spacer(Modifier.size(8.dp))
                    SmallFloatingActionButton(
                        onClick = { isPtzOverlayVisible = true },
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Show PTZ controls"
                            },
                    ) {
                        PtzControlsIcon(Modifier.size(24.dp))
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
                audioDiagnostics = audioDiagnostics,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
            )
        }

        if (!audioMuted && !isPtzOverlayVisible) {
            AudioMeter(
                levels = audioLevels,
                status = audioStatus,
                showDbLabels = showAudioDbLabels,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
            )
        }

        PlaybackControlPanel(
            isVisible = isPlaybackOverlayVisible,
            bandwidth = bandwidth,
            audioStatus = audioStatus,
            audioVolume = audioVolume,
            audioMuted = audioMuted,
            showAudioDbLabels = showAudioDbLabels,
            onBandwidthChanged = { value ->
                bandwidth = value
                automaticFallbackToLow = false
            },
            onAudioMutedChanged = { value ->
                audioMuted = value
                if (!isDeveloperExample) playerController.setAudioMuted(value)
            },
            onAudioVolumeChanged = { value ->
                audioVolume = value
                if (!isDeveloperExample) playerController.setAudioVolume(value)
            },
            onShowAudioDbLabelsChanged = { value -> showAudioDbLabels = value },
            onRetryAudioFocus = {
                if (!isDeveloperExample) playerController.retryAudioFocus()
            },
            onClose = { isPlaybackOverlayVisible = false },
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp),
        )

        PtzControlPanel(
            isSupported = isPtzSupported && isPtzOverlayVisible,
            onPanTiltSpeed = { pan, tilt ->
                runPtzCommand("Pan/tilt movement") { playerController.panTiltSpeed(pan, tilt) }
            },
            onZoomSpeed = { speed ->
                runPtzCommand("Zoom movement") { playerController.zoomSpeed(speed) }
            },
            onFocusSpeed = { speed ->
                runPtzCommand("Focus movement") { playerController.focusSpeed(speed) }
            },
            onStop = {
                if (!isDeveloperExample) playerController.stopPtzMovement()
            },
            onAutoFocus = {
                runPtzCommand("Autofocus") { playerController.autoFocus() }
            },
            onToggleVisibility = { isPtzOverlayVisible = false },
            onRecallPreset = { presetNumber ->
                if (isDeveloperExample) {
                    ptzStatusMessage = "Demo: preset $presetNumber recall accepted."
                } else {
                    coroutineScope.launch {
                        ptzStatusMessage =
                            playerController
                                .recallPtzPreset(presetNumber)
                                .presetStatusMessage("Preset $presetNumber recall")
                    }
                }
            },
            onStorePreset = { presetNumber ->
                if (isDeveloperExample) {
                    ptzStatusMessage = "Demo: preset $presetNumber store accepted."
                } else {
                    coroutineScope.launch {
                        ptzStatusMessage =
                            playerController
                                .storePtzPreset(presetNumber)
                                .presetStatusMessage("Preset $presetNumber store")
                    }
                }
            },
            onClearPreset = { presetNumber ->
                ptzStatusMessage =
                    if (isDeveloperExample) {
                        "Demo: preset $presetNumber cleared."
                    } else {
                        "Preset clearing is not supported by the NDI® PTZ API."
                    }
            },
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp),
        )

        ptzStatusMessage?.let { message ->
            Surface(
                color = Color.Black.copy(alpha = 0.84f),
                contentColor = Color.White,
                shape =
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(12.dp),
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .semantics { contentDescription = "PTZ command status" },
            ) {
                Text(message, modifier = Modifier.padding(12.dp))
            }
        }
    }

    DisposableEffect(source) {
        onDispose {
            if (!isDeveloperExample) {
                playerController.stopPtzMovement()
                playerController.stop()
            }
        }
    }
}
