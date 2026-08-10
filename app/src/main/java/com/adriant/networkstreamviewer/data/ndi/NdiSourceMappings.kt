package com.adriant.networkstreamviewer.data.ndi

import com.adriant.networkstreamviewer.domain.model.NdiSource
import com.adriant.networkstreamviewer.domain.model.NdiStreamDetails
import com.adriant.networkstreamviewer.domain.model.NdiVideoFormat

internal fun Array<String>.toNdiSources(): List<NdiSource> =
    asList().chunked(SOURCE_VALUE_COUNT).mapNotNull { values ->
        if (values.size == SOURCE_VALUE_COUNT) {
            NdiSource(name = values[0], url = values[1])
        } else {
            null
        }
    }

internal fun IntArray?.toStreamDetails(): NdiStreamDetails? {
    if (this == null || size != STREAM_DETAILS_VALUE_COUNT) return null
    val (width, height, frameRateNumerator, frameRateDenominator, formatCode) = this
    if (width <= 0 || height <= 0 || frameRateNumerator <= 0 || frameRateDenominator <= 0) return null

    val format =
        when (formatCode) {
            1 -> NdiVideoFormat.HX_H264
            2 -> NdiVideoFormat.HX_HEVC
            else -> NdiVideoFormat.FULL_NDI
        }
    return NdiStreamDetails(
        width = width,
        height = height,
        frameRateNumerator = frameRateNumerator,
        frameRateDenominator = frameRateDenominator,
        format = format,
    )
}

private const val SOURCE_VALUE_COUNT = 2
private const val STREAM_DETAILS_VALUE_COUNT = 5
