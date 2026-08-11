package com.adriant.networkstreamviewer.data.ndi

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Surface
import com.adriant.networkstreamviewer.domain.model.NdiAudioDiagnostics
import com.adriant.networkstreamviewer.domain.model.NdiAudioLevels
import com.adriant.networkstreamviewer.domain.model.NdiAudioStatus
import com.adriant.networkstreamviewer.domain.model.NdiBandwidth
import com.adriant.networkstreamviewer.domain.model.NdiPlaybackState
import com.adriant.networkstreamviewer.domain.model.NdiPtzCommandResult
import com.adriant.networkstreamviewer.domain.model.NdiSource
import com.adriant.networkstreamviewer.domain.model.NdiVideoDiagnostics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

class NdiPlayerController(
    context: Context,
) {
    private val applicationContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile private var audioPlayer: NdiAudioPlayer? = null

    @Volatile private var audioVolume = 1f

    @Volatile private var audioMuted = false
    private val lifecycleScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lifecycleMutex = Mutex()
    private val operationGeneration = AtomicLong(0L)

    fun start(
        source: NdiSource,
        surface: Surface,
        bandwidth: NdiBandwidth,
        onAspectRatioChanged: (Float) -> Unit,
        onVideoDiagnosticsChanged: (NdiVideoDiagnostics) -> Unit = {},
        onPlaybackStateChanged: (NdiPlaybackState) -> Unit,
        onPtzSupportChanged: (Boolean) -> Unit,
        onStartFailed: () -> Unit,
        initialAudioVolume: Float = audioVolume,
        initialAudioMuted: Boolean = audioMuted,
        onAudioStatusChanged: (NdiAudioStatus) -> Unit = {},
        onAudioLevelsChanged: (NdiAudioLevels) -> Unit = {},
        onAudioDiagnosticsChanged: (NdiAudioDiagnostics) -> Unit = {},
    ) {
        val operation = operationGeneration.incrementAndGet()
        lifecycleScope.launch {
            lifecycleMutex.withLock {
                if (operation != operationGeneration.get()) return@withLock

                val started =
                    try {
                        NdiNative.startReceiver(
                            source.name,
                            source.url,
                            surface,
                            bandwidth.toNativeValue(),
                            object : NdiPlaybackListener {
                                override fun onVideoAspectRatioChanged(aspectRatio: Float) {
                                    if (operation == operationGeneration.get()) {
                                        onAspectRatioChanged(aspectRatio)
                                    }
                                }

                                override fun onVideoDiagnosticsChanged(
                                    totalFrames: Long,
                                    droppedFrames: Long,
                                    width: Int,
                                    height: Int,
                                    queueDepth: Int,
                                    receivedFps: Float,
                                    renderedFps: Float,
                                    processingTimeMs: Float,
                                ) {
                                    if (operation == operationGeneration.get()) {
                                        onVideoDiagnosticsChanged(
                                            NdiVideoDiagnostics(
                                                width = width,
                                                height = height,
                                                receivedFrames = totalFrames,
                                                receivedFps = receivedFps,
                                                renderedFps = renderedFps,
                                                droppedFrames = droppedFrames,
                                                queueDepth = queueDepth,
                                                processingTimeMs = processingTimeMs,
                                            ),
                                        )
                                    }
                                }

                                override fun onPlaybackStateChanged(state: Int) {
                                    if (operation == operationGeneration.get()) {
                                        onPlaybackStateChanged(state.toPlaybackState())
                                    }
                                }

                                override fun onPtzSupportChanged(isSupported: Boolean) {
                                    if (operation == operationGeneration.get()) {
                                        onPtzSupportChanged(isSupported)
                                    }
                                }
                            },
                        )
                    } catch (_: Exception) {
                        false
                    }

                if (started && operation == operationGeneration.get()) {
                    val audio =
                        NdiAudioPlayer(
                            context = applicationContext,
                            onStatusChanged = { value ->
                                postAudioCallback(operation) { onAudioStatusChanged(value) }
                            },
                            onLevelsChanged = { value ->
                                postAudioCallback(operation) { onAudioLevelsChanged(value) }
                            },
                            onDiagnosticsChanged = { value ->
                                postAudioCallback(operation) { onAudioDiagnosticsChanged(value) }
                            },
                        )
                    audio.setVolume(initialAudioVolume)
                    audio.setMuted(initialAudioMuted)
                    audioPlayer = audio
                    audio.start()
                } else if (!started && operation == operationGeneration.get()) {
                    onStartFailed()
                }
            }
        }
    }

    suspend fun recallPtzPreset(
        presetNumber: Int,
        speed: Float = MAX_PRESET_SPEED,
    ): NdiPtzCommandResult =
        withContext(Dispatchers.IO) {
            if (!isValidPtzPreset(presetNumber) || !isValidPtzSpeed(speed)) {
                return@withContext NdiPtzCommandResult.INVALID_ARGUMENT
            }
            NdiNative.recallPtzPreset(presetNumber, speed).toPtzCommandResult()
        }

    suspend fun storePtzPreset(presetNumber: Int): NdiPtzCommandResult =
        withContext(Dispatchers.IO) {
            if (!isValidPtzPreset(presetNumber)) {
                return@withContext NdiPtzCommandResult.INVALID_ARGUMENT
            }
            NdiNative.storePtzPreset(presetNumber).toPtzCommandResult()
        }

    suspend fun panTiltSpeed(
        panSpeed: Float,
        tiltSpeed: Float,
    ): NdiPtzCommandResult =
        withContext(Dispatchers.IO) {
            if (!isValidPtzMovementSpeed(panSpeed) || !isValidPtzMovementSpeed(tiltSpeed)) {
                return@withContext NdiPtzCommandResult.INVALID_ARGUMENT
            }
            NdiNative.panTiltSpeed(panSpeed, tiltSpeed).toPtzCommandResult()
        }

    suspend fun zoomSpeed(speed: Float): NdiPtzCommandResult =
        withContext(Dispatchers.IO) {
            if (!isValidPtzMovementSpeed(speed) || kotlin.math.abs(speed) > MAX_ZOOM_SPEED) {
                return@withContext NdiPtzCommandResult.INVALID_ARGUMENT
            }
            NdiNative.zoomSpeed(speed).toPtzCommandResult()
        }

    suspend fun focusSpeed(speed: Float): NdiPtzCommandResult =
        withContext(Dispatchers.IO) {
            if (!isValidPtzMovementSpeed(speed) || kotlin.math.abs(speed) > MAX_FOCUS_SPEED) {
                return@withContext NdiPtzCommandResult.INVALID_ARGUMENT
            }
            NdiNative.focusSpeed(speed).toPtzCommandResult()
        }

    suspend fun focus(value: Float): NdiPtzCommandResult =
        withContext(Dispatchers.IO) {
            if (!isValidPtzAbsoluteValue(value)) {
                return@withContext NdiPtzCommandResult.INVALID_ARGUMENT
            }
            NdiNative.focus(value).toPtzCommandResult()
        }

    suspend fun autoFocus(): NdiPtzCommandResult =
        withContext(Dispatchers.IO) {
            NdiNative.autoFocus().toPtzCommandResult()
        }

    suspend fun whiteBalanceAuto(): NdiPtzCommandResult =
        withContext(Dispatchers.IO) {
            NdiNative.whiteBalanceAuto().toPtzCommandResult()
        }

    suspend fun whiteBalanceIndoor(): NdiPtzCommandResult =
        withContext(Dispatchers.IO) {
            NdiNative.whiteBalanceIndoor().toPtzCommandResult()
        }

    suspend fun whiteBalanceOutdoor(): NdiPtzCommandResult =
        withContext(Dispatchers.IO) {
            NdiNative.whiteBalanceOutdoor().toPtzCommandResult()
        }

    suspend fun whiteBalanceOneShot(): NdiPtzCommandResult =
        withContext(Dispatchers.IO) {
            NdiNative.whiteBalanceOneShot().toPtzCommandResult()
        }

    suspend fun whiteBalanceManual(
        red: Float,
        blue: Float,
    ): NdiPtzCommandResult =
        withContext(Dispatchers.IO) {
            if (!isValidPtzAbsoluteValue(red) || !isValidPtzAbsoluteValue(blue)) {
                return@withContext NdiPtzCommandResult.INVALID_ARGUMENT
            }
            NdiNative.whiteBalanceManual(red, blue).toPtzCommandResult()
        }

    fun setAudioVolume(volume: Float) {
        audioVolume = if (volume.isFinite()) volume.coerceIn(0f, 1f) else 0f
        audioPlayer?.setVolume(audioVolume)
    }

    fun setAudioMuted(muted: Boolean) {
        audioMuted = muted
        audioPlayer?.setMuted(muted)
    }

    fun retryAudioFocus() {
        audioPlayer?.retryAudioFocus()
    }

    /** Sends a stop even when the composable scope is being cancelled. */
    fun stopPtzMovement() {
        lifecycleScope.launch {
            NdiNative.stopPtzMovement()
        }
    }

    fun stop() {
        enqueueStop(closeScope = false)
    }

    fun close() {
        enqueueStop(closeScope = true)
    }

    private fun enqueueStop(closeScope: Boolean) {
        val operation = operationGeneration.incrementAndGet()
        val stopJob =
            lifecycleScope.launch {
                lifecycleMutex.withLock {
                    if (operation != operationGeneration.get()) return@withLock
                    stopPlayback()
                }
            }
        if (closeScope) {
            stopJob.invokeOnCompletion { lifecycleScope.cancel() }
        }
    }

    private fun stopPlayback() {
        audioPlayer?.stop()
        audioPlayer = null
        NdiNative.stopReceiver()
    }

    private fun postAudioCallback(
        operation: Long,
        callback: () -> Unit,
    ) {
        if (operation != operationGeneration.get()) return
        mainHandler.post {
            if (operation == operationGeneration.get()) callback()
        }
    }

    private companion object {
        const val MAX_PRESET_SPEED = 1.0f
        const val MAX_ZOOM_SPEED = 0.5f
        const val MAX_FOCUS_SPEED = 0.5f
    }
}
