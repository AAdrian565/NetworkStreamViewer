package com.adriant.networkstreamviewer.domain.model

enum class CameraLens(val label: String) {
    BACK("Back camera"),
    FRONT("Front camera")
}

enum class CameraResolution(
    val label: String,
    val width: Int,
    val height: Int
) {
    SD("480p", 640, 480),
    HD("720p", 1280, 720),
    FULL_HD("1080p", 1920, 1080)
}

data class CameraSenderSettings(
    val resolution: CameraResolution = CameraResolution.HD,
    val frameRate: Int = 30,
    val lens: CameraLens = CameraLens.BACK
)

val cameraFrameRateOptions: List<Int> = listOf(15, 24, 30)
