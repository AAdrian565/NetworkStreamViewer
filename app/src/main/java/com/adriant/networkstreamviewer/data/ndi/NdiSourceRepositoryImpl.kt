package com.adriant.networkstreamviewer.data.ndi

import com.adriant.networkstreamviewer.domain.model.NdiSource
import com.adriant.networkstreamviewer.domain.model.NdiStreamDetails
import com.adriant.networkstreamviewer.domain.repository.NdiSourceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NdiSourceRepositoryImpl : NdiSourceRepository {
    override suspend fun initialize(): Boolean =
        withContext(Dispatchers.IO) {
            NdiNative.initialize()
        }

    override suspend fun discoverSources(timeoutMs: Int): List<NdiSource> =
        withContext(Dispatchers.IO) {
            NdiNative.discoverSources(timeoutMs).toNdiSources()
        }

    override suspend fun probeSource(source: NdiSource): NdiStreamDetails? =
        withContext(Dispatchers.IO) {
            NdiNative.probeSource(source.name, source.url, PROBE_TIMEOUT_MS).toStreamDetails()
        }

    override suspend fun shutdown() =
        withContext(Dispatchers.IO) {
            NdiNative.shutdown()
        }
}

private const val PROBE_TIMEOUT_MS = 1_500
