package com.adriant.networkstreamviewer.domain.repository

import com.adriant.networkstreamviewer.domain.model.NdiSource
import com.adriant.networkstreamviewer.domain.model.NdiStreamDetails

interface NdiSourceRepository {
    suspend fun initialize(): Boolean

    suspend fun discoverSources(timeoutMs: Int): List<NdiSource>

    suspend fun probeSource(source: NdiSource): NdiStreamDetails?

    suspend fun shutdown()
}
