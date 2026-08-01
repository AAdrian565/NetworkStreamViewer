package com.adriant.networkstreamviewer.data.ndi

import android.view.Surface
import com.adriant.networkstreamviewer.domain.model.NdiBandwidth
import com.adriant.networkstreamviewer.domain.model.NdiPlaybackState
import com.adriant.networkstreamviewer.domain.model.NdiSource

class NdiPlayerController {
    fun start(
        source: NdiSource,
        surface: Surface,
        bandwidth: NdiBandwidth,
        onAspectRatioChanged: (Float) -> Unit,
        onPlaybackStateChanged: (NdiPlaybackState) -> Unit
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
        }
    )

    fun stop() {
        NdiNative.stopReceiver()
    }
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
