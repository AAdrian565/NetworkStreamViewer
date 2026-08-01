package com.adriant.networkstreamviewer.data.ndi

import com.adriant.networkstreamviewer.domain.model.NdiSource
import com.adriant.networkstreamviewer.domain.model.NdiStreamDetails
import com.adriant.networkstreamviewer.domain.model.NdiVideoFormat
import com.adriant.networkstreamviewer.domain.repository.NdiSourceRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

class NdiSourceRepositoryImpl : NdiSourceRepository {
    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        NdiNative.initialize()
    }

    override suspend fun discoverSources(timeoutMs: Int): List<NdiSource> =
        withContext(Dispatchers.IO) {
            val sources = NdiNative.discoverSources(timeoutMs).toNdiSources()
            val probeSlots = Semaphore(MAX_CONCURRENT_PROBES)
            coroutineScope {
                sources.map { source ->
                    async {
                        probeSlots.withPermit {
                            source.copy(
                                details = NdiNative.probeSource(
                                    source.name,
                                    source.url,
                                    PROBE_TIMEOUT_MS
                                ).toStreamDetails()
                            )
                        }
                    }
                }.awaitAll()
            }
        }

    override fun shutdown() {
        NdiNative.shutdown()
    }
}

internal fun Array<String>.toNdiSources(): List<NdiSource> =
    asList().chunked(2).mapNotNull { values ->
        if (values.size == 2) NdiSource(name = values[0], url = values[1]) else null
    }

internal fun IntArray?.toStreamDetails(): NdiStreamDetails? {
    if (this == null || size != STREAM_DETAILS_VALUE_COUNT) return null
    val (width, height, frameRateNumerator, frameRateDenominator, formatCode) = this
    if (width <= 0 || height <= 0 || frameRateNumerator <= 0 || frameRateDenominator <= 0) return null
    val format = when (formatCode) {
        1 -> NdiVideoFormat.HX_H264
        2 -> NdiVideoFormat.HX_HEVC
        else -> NdiVideoFormat.FULL_NDI
    }
    return NdiStreamDetails(
        width = width,
        height = height,
        frameRateNumerator = frameRateNumerator,
        frameRateDenominator = frameRateDenominator,
        format = format
    )
}

private const val MAX_CONCURRENT_PROBES = 3
private const val PROBE_TIMEOUT_MS = 1_500
private const val STREAM_DETAILS_VALUE_COUNT = 5
