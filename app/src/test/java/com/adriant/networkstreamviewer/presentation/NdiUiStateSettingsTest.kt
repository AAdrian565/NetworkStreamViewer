package com.adriant.networkstreamviewer.presentation

import com.adriant.networkstreamviewer.domain.model.AppSettings
import com.adriant.networkstreamviewer.domain.model.DEVELOPER_SOURCE_URL
import com.adriant.networkstreamviewer.domain.model.DiscoveryRefreshInterval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NdiUiStateSettingsTest {
    @Test
    fun `developer mode adds example source without changing discovered sources`() {
        val state = NdiUiState(
            isInitialized = true,
            hasCompletedRefresh = true,
            settings = AppSettings(developerMode = true)
        )

        assertTrue(state.sources.isEmpty())
        assertEquals(1, state.displayedSources.size)
        assertEquals(DEVELOPER_SOURCE_URL, state.displayedSources.single().url)
        assertEquals("1 source found", state.statusMessage)
    }

    @Test
    fun `manual discovery has no interval and periodic choices have positive intervals`() {
        assertEquals(null, DiscoveryRefreshInterval.MANUAL.intervalMillis)
        DiscoveryRefreshInterval.entries
            .filterNot { it == DiscoveryRefreshInterval.MANUAL }
            .forEach { assertTrue(it.intervalMillis.orEmpty() > 0L) }
    }

    private fun Long?.orEmpty(): Long = this ?: 0L
}
