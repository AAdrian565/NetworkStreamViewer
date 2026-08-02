package com.adriant.networkstreamviewer.data.ndi

import android.view.Surface
import com.adriant.networkstreamviewer.domain.model.NdiBandwidth
import com.adriant.networkstreamviewer.domain.model.NdiPlaybackState
import com.adriant.networkstreamviewer.domain.model.NdiPtzCommandResult
import com.adriant.networkstreamviewer.domain.model.NdiSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NdiPlayerController {
    fun start(
        source: NdiSource,
        surface: Surface,
        bandwidth: NdiBandwidth,
        onAspectRatioChanged: (Float) -> Unit,
        onPlaybackStateChanged: (NdiPlaybackState) -> Unit,
        onPtzSupportChanged: (Boolean) -> Unit
    ): Boolean = NdiNative.startReceiver(
        source.name,
        source.url,
        surface,
        bandwidth.toNativeValue(),
        object : NdiPlaybackListener {
            override fun onVideoAspectRatioChanged(aspectRatio: Float) {
                onAspectRatioChanged(aspectRatio)
            }

            override fun onPlaybackStateChanged(state: Int) {
                onPlaybackStateChanged(state.toPlaybackState())
            }

            override fun onPtzSupportChanged(isSupported: Boolean) {
                onPtzSupportChanged(isSupported)
            }
        }
    )

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
        NdiNative.stopReceiver()
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
