package com.adriant.networkstreamviewer.data.ndi

import com.adriant.networkstreamviewer.domain.model.NdiSource
import com.adriant.networkstreamviewer.domain.repository.NdiSourceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NdiSourceRepositoryImpl : NdiSourceRepository {
    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        NdiNative.initialize()
    }

    override suspend fun discoverSources(timeoutMs: Int): List<NdiSource> =
        withContext(Dispatchers.IO) {
            NdiNative.discoverSources(timeoutMs).toNdiSources()
        }

    override fun shutdown() {
        NdiNative.shutdown()
    }
}

internal fun Array<String>.toNdiSources(): List<NdiSource> =
    asList().chunked(2).mapNotNull { values ->
        if (values.size == 2) NdiSource(name = values[0], url = values[1]) else null
    }
