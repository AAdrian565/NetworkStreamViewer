package com.adriant.networkstreamviewer.domain.model

data class NdiAudioLevels(
    val leftPeakDbfs: Float = -60f,
    val rightPeakDbfs: Float = -60f,
    val leftRmsDbfs: Float = -60f,
    val rightRmsDbfs: Float = -60f,
    val leftClipped: Boolean = false,
    val rightClipped: Boolean = false,
) {
    companion object {
        val FLOOR = NdiAudioLevels()
    }
}
