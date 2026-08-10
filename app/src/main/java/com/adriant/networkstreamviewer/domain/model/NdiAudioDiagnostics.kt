package com.adriant.networkstreamviewer.domain.model

data class NdiAudioDiagnostics(
    val status: NdiAudioStatus = NdiAudioStatus.STARTING,
    val outputSampleRate: Int = 48_000,
    val outputChannelCount: Int = 2,
    val totalFrames: Long = 0,
    val droppedFrames: Long = 0,
    val underrunCount: Int = 0
)
