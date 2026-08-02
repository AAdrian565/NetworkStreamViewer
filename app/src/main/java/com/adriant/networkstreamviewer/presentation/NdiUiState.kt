package com.adriant.networkstreamviewer.presentation

import com.adriant.networkstreamviewer.domain.model.AppSettings
import com.adriant.networkstreamviewer.domain.model.NdiSource
import com.adriant.networkstreamviewer.domain.model.developerExampleSource

data class NdiUiState(
    val isInitialized: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasCompletedRefresh: Boolean = false,
    val sources: List<NdiSource> = emptyList(),
    val selectedSource: NdiSource? = null,
    val isCameraSenderOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isAboutOpen: Boolean = false,
    val areDeveloperOptionsUnlocked: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val errorMessage: String? = null
) {
    val displayedSources: List<NdiSource>
        get() = if (settings.developerMode) sources + developerExampleSource else sources

    val statusMessage: String
        get() = when {
            errorMessage != null -> errorMessage
            isRefreshing -> "Searching for NDI® sources…"
            !isInitialized -> "Waiting to start discovery…"
            hasCompletedRefresh && displayedSources.isEmpty() ->
                "No streams found. Pull down to refresh."
            else -> "${displayedSources.size} source${if (displayedSources.size == 1) "" else "s"} found"
        }
}
