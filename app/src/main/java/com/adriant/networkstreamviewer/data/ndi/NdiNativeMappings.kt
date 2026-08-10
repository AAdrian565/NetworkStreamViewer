package com.adriant.networkstreamviewer.data.ndi

import com.adriant.networkstreamviewer.domain.model.NdiBandwidth
import com.adriant.networkstreamviewer.domain.model.NdiPlaybackState
import com.adriant.networkstreamviewer.domain.model.NdiPtzCommandResult

internal fun isValidPtzPreset(presetNumber: Int): Boolean = presetNumber in 0..99

internal fun isValidPtzSpeed(speed: Float): Boolean = speed.isFinite() && speed in 0.0f..1.0f

internal fun isValidPtzMovementSpeed(speed: Float): Boolean = speed.isFinite() && speed in -1.0f..1.0f

internal fun isValidPtzAbsoluteValue(value: Float): Boolean = value.isFinite() && value in 0.0f..1.0f

internal fun Int.toPtzCommandResult(): NdiPtzCommandResult =
    when (this) {
        0 -> NdiPtzCommandResult.ACCEPTED
        1 -> NdiPtzCommandResult.UNAVAILABLE
        2 -> NdiPtzCommandResult.REJECTED
        3 -> NdiPtzCommandResult.INVALID_ARGUMENT
        else -> NdiPtzCommandResult.REJECTED
    }

internal fun NdiBandwidth.toNativeValue(): Int =
    when (this) {
        NdiBandwidth.AUTOMATIC -> 0
        NdiBandwidth.HIGHEST -> 1
        NdiBandwidth.LOWEST -> 2
    }

internal fun Int.toPlaybackState(): NdiPlaybackState =
    when (this) {
        0 -> NdiPlaybackState.CONNECTING
        1 -> NdiPlaybackState.WAITING_FOR_KEYFRAME
        2 -> NdiPlaybackState.PLAYING
        3 -> NdiPlaybackState.DISCONNECTED
        4 -> NdiPlaybackState.UNSUPPORTED_CODEC
        5 -> NdiPlaybackState.DECODER_FAILURE
        6 -> NdiPlaybackState.INSUFFICIENT_BANDWIDTH
        else -> NdiPlaybackState.DECODER_FAILURE
    }
