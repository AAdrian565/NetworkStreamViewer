package com.adriant.networkstreamviewer.domain.model

data class NdiSource(
    val name: String,
    val url: String,
    val details: NdiStreamDetails? = null,
    val isLoadingDetails: Boolean = true
)

const val DEVELOPER_SOURCE_URL = "dev://example-empty-stream"

val developerExampleSource = NdiSource(
    name = "Example empty stream (Developer)",
    url = DEVELOPER_SOURCE_URL,
    details = NdiStreamDetails(
        width = 1920,
        height = 1080,
        frameRateNumerator = 60,
        frameRateDenominator = 1,
        format = NdiVideoFormat.HX_H264
    ),
    isLoadingDetails = false
)

val NdiSource.isDeveloperExample: Boolean
    get() = url == DEVELOPER_SOURCE_URL
