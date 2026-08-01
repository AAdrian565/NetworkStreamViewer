package com.adriant.networkstreamviewer.domain.model

enum class NdiPlaybackState {
    CONNECTING,
    WAITING_FOR_KEYFRAME,
    PLAYING,
    DISCONNECTED,
    UNSUPPORTED_CODEC,
    DECODER_FAILURE,
    INSUFFICIENT_BANDWIDTH
}
