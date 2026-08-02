package com.adriant.networkstreamviewer.domain.model

enum class AppTheme(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("AMOLED")
}

enum class DiscoveryRefreshInterval(
    val label: String,
    val intervalMillis: Long?
) {
    MANUAL("Manual only", null),
    THIRTY_SECONDS("Every 30 seconds", 30_000L),
    ONE_MINUTE("Every minute", 60_000L),
    FIVE_MINUTES("Every 5 minutes", 300_000L)
}

data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val keepScreenAwake: Boolean = true,
    val showPlaybackDiagnostics: Boolean = false,
    val developerMode: Boolean = false,
    val discoveryRefreshInterval: DiscoveryRefreshInterval = DiscoveryRefreshInterval.MANUAL
)
