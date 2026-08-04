package com.adriant.networkstreamviewer.data.ndi

import android.view.Surface
import com.adriant.networkstreamviewer.domain.model.NdiBandwidth
import com.adriant.networkstreamviewer.domain.model.NdiPlaybackState
import com.adriant.networkstreamviewer.domain.model.NdiPtzCommandResult
import com.adriant.networkstreamviewer.domain.model.NdiSource
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class NdiPlayerController {
    private val lifecycleScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lifecycleMutex = Mutex()
    private val operationGeneration = AtomicLong(0L)

    fun start(
        source: NdiSource,
        surface: Surface,
        bandwidth: NdiBandwidth,
        onAspectRatioChanged: (Float) -> Unit,
        onPlaybackStateChanged: (NdiPlaybackState) -> Unit,
        onPtzSupportChanged: (Boolean) -> Unit,
        onStartFailed: () -> Unit
    ) {
        val operation = operationGeneration.incrementAndGet()
        lifecycleScope.launch {
            lifecycleMutex.withLock {
                if (operation != operationGeneration.get()) return@withLock

                val started = try {
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
                        }
                    )
                } catch (_: Exception) {
                    false
                }

                if (!started && operation == operationGeneration.get()) {
                    onStartFailed()
                }
            }
        }
    }

    suspend fun recallPtzPreset(
        presetNumber: Int,
        speed: Float = MAX_PRESET_SPEED
    ): NdiPtzCommandResult = withContext(Dispatchers.IO) {
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

    fun stop() {
        val operation = operationGeneration.incrementAndGet()
        lifecycleScope.launch {
            lifecycleMutex.withLock {
                if (operation != operationGeneration.get()) return@withLock
                NdiNative.stopReceiver()
            }
        }
    }

    fun close() {
        val operation = operationGeneration.incrementAndGet()
        lifecycleScope.launch {
            lifecycleMutex.withLock {
                if (operation != operationGeneration.get()) return@withLock
                NdiNative.stopReceiver()
            }
        }.invokeOnCompletion {
            lifecycleScope.cancel()
        }
    }

    private companion object {
        const val MAX_PRESET_SPEED = 1.0f
    }
}

internal fun isValidPtzPreset(presetNumber: Int): Boolean = presetNumber in 0..99

internal fun isValidPtzSpeed(speed: Float): Boolean = speed.isFinite() && speed in 0.0f..1.0f

internal fun Int.toPtzCommandResult(): NdiPtzCommandResult = when (this) {
    0 -> NdiPtzCommandResult.ACCEPTED
    1 -> NdiPtzCommandResult.UNAVAILABLE
    2 -> NdiPtzCommandResult.REJECTED
    3 -> NdiPtzCommandResult.INVALID_ARGUMENT
    else -> NdiPtzCommandResult.REJECTED
}

internal fun NdiBandwidth.toNativeValue(): Int = when (this) {
    NdiBandwidth.AUTOMATIC -> 0
    NdiBandwidth.HIGHEST -> 1
    NdiBandwidth.LOWEST -> 2
}

internal fun Int.toPlaybackState(): NdiPlaybackState = when (this) {
    0 -> NdiPlaybackState.CONNECTING
    1 -> NdiPlaybackState.WAITING_FOR_KEYFRAME
    2 -> NdiPlaybackState.PLAYING
    3 -> NdiPlaybackState.DISCONNECTED
    4 -> NdiPlaybackState.UNSUPPORTED_CODEC
    5 -> NdiPlaybackState.DECODER_FAILURE
    6 -> NdiPlaybackState.INSUFFICIENT_BANDWIDTH
    else -> NdiPlaybackState.DECODER_FAILURE
}
