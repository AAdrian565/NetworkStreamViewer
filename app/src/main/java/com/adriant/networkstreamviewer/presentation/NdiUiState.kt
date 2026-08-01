package com.adriant.networkstreamviewer.presentation

import com.adriant.networkstreamviewer.domain.model.NdiSource

data class NdiUiState(
    val isInitialized: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasCompletedRefresh: Boolean = false,
    val sources: List<NdiSource> = emptyList(),
    val selectedSource: NdiSource? = null,
    val errorMessage: String? = null
) {
    val statusMessage: String
        get() = when {
            errorMessage != null -> errorMessage
            isRefreshing -> "Searching for NDI® sources…"
            !isInitialized -> "Waiting to start discovery…"
            hasCompletedRefresh && sources.isEmpty() -> "No streams found. Pull down to refresh."
            else -> "${sources.size} source${if (sources.size == 1) "" else "s"} found"
        }
}
