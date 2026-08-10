package com.adriant.networkstreamviewer.presentation.player

import com.adriant.networkstreamviewer.domain.model.NdiBandwidth
import com.adriant.networkstreamviewer.domain.model.NdiPlaybackState
import com.adriant.networkstreamviewer.domain.model.NdiPtzCommandResult

internal val NdiBandwidth.label: String
    get() =
        when (this) {
            NdiBandwidth.AUTOMATIC -> "Automatic"
            NdiBandwidth.HIGHEST -> "Highest"
            NdiBandwidth.LOWEST -> "Preview / Low"
        }

internal val NdiBandwidth.shortLabel: String
    get() =
        when (this) {
            NdiBandwidth.AUTOMATIC -> "Auto"
            NdiBandwidth.HIGHEST -> "High"
            NdiBandwidth.LOWEST -> "Low"
        }

internal val NdiPlaybackState.label: String
    get() =
        when (this) {
            NdiPlaybackState.CONNECTING -> "Connecting"
            NdiPlaybackState.WAITING_FOR_KEYFRAME -> "Waiting for keyframe"
            NdiPlaybackState.PLAYING -> "Playing"
            NdiPlaybackState.DISCONNECTED -> "Disconnected"
            NdiPlaybackState.UNSUPPORTED_CODEC -> "Unsupported codec"
            NdiPlaybackState.DECODER_FAILURE -> "Decoder failure"
            NdiPlaybackState.INSUFFICIENT_BANDWIDTH -> "Insufficient bandwidth"
        }

internal fun NdiPtzCommandResult.presetStatusMessage(action: String): String =
    when (this) {
        NdiPtzCommandResult.ACCEPTED -> "$action accepted."
        NdiPtzCommandResult.UNAVAILABLE -> "$action unavailable; the camera is not connected or PTZ-capable."
        NdiPtzCommandResult.REJECTED -> "$action was rejected by the camera."
        NdiPtzCommandResult.INVALID_ARGUMENT -> "$action has an invalid preset number or speed."
    }

internal fun NdiPtzCommandResult.ptzStatusMessage(action: String): String =
    when (this) {
        NdiPtzCommandResult.ACCEPTED -> "$action accepted."
        NdiPtzCommandResult.UNAVAILABLE -> "$action unavailable; the camera is not connected or PTZ-capable."
        NdiPtzCommandResult.REJECTED -> "$action was rejected by the camera."
        NdiPtzCommandResult.INVALID_ARGUMENT -> "$action has an invalid PTZ value."
    }
