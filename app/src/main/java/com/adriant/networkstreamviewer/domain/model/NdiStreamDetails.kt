package com.adriant.networkstreamviewer.domain.model

data class NdiStreamDetails(
    val width: Int,
    val height: Int,
    val frameRateNumerator: Int,
    val frameRateDenominator: Int,
    val format: NdiVideoFormat,
)

enum class NdiVideoFormat {
    FULL_NDI,
    HX_H264,
    HX_HEVC,
}
