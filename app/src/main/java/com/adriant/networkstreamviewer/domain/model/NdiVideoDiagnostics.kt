package com.adriant.networkstreamviewer.domain.model

data class NdiVideoDiagnostics(
    val width: Int = 0,
    val height: Int = 0,
    val receivedFrames: Long = 0,
    val receivedFps: Float = 0f,
    val renderedFps: Float = 0f,
    val droppedFrames: Long = 0,
    val queueDepth: Int = 0,
    val processingTimeMs: Float = 0f,
)
