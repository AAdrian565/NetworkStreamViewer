package com.adriant.networkstreamviewer.domain.repository

import com.adriant.networkstreamviewer.domain.model.NdiSource

interface NdiSourceRepository {
    suspend fun initialize(): Boolean
    suspend fun discoverSources(timeoutMs: Int): List<NdiSource>
    fun shutdown()
}
