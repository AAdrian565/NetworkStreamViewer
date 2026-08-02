package com.adriant.networkstreamviewer.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.adriant.networkstreamviewer.domain.model.AppSettings
import com.adriant.networkstreamviewer.domain.model.AppTheme
import com.adriant.networkstreamviewer.domain.model.DiscoveryRefreshInterval
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableSettings = MutableStateFlow(preferences.toAppSettings())
    val settings: StateFlow<AppSettings> = mutableSettings.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, _ ->
        mutableSettings.value = sharedPreferences.toAppSettings()
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setTheme(theme: AppTheme) = edit { putString(KEY_THEME, theme.name) }

    fun setKeepScreenAwake(enabled: Boolean) = edit { putBoolean(KEY_KEEP_SCREEN_AWAKE, enabled) }

    fun setShowPlaybackDiagnostics(enabled: Boolean) =
        edit { putBoolean(KEY_SHOW_PLAYBACK_DIAGNOSTICS, enabled) }

    fun setDeveloperMode(enabled: Boolean) = edit { putBoolean(KEY_DEVELOPER_MODE, enabled) }

    fun setDiscoveryRefreshInterval(interval: DiscoveryRefreshInterval) =
        edit { putString(KEY_DISCOVERY_REFRESH_INTERVAL, interval.name) }

    private fun edit(action: SharedPreferences.Editor.() -> Unit) {
        preferences.edit().apply(action).apply()
    }

    private fun SharedPreferences.toAppSettings() = AppSettings(
        theme = enumValueOrDefault(getString(KEY_THEME, null), AppTheme.SYSTEM),
        keepScreenAwake = getBoolean(KEY_KEEP_SCREEN_AWAKE, true),
        showPlaybackDiagnostics = getBoolean(KEY_SHOW_PLAYBACK_DIAGNOSTICS, false),
        developerMode = getBoolean(KEY_DEVELOPER_MODE, false),
        discoveryRefreshInterval = enumValueOrDefault(
            getString(KEY_DISCOVERY_REFRESH_INTERVAL, null),
            DiscoveryRefreshInterval.MANUAL
        )
    )

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default

    private companion object {
        const val PREFERENCES_NAME = "app_settings"
        const val KEY_THEME = "theme"
        const val KEY_KEEP_SCREEN_AWAKE = "keep_screen_awake"
        const val KEY_SHOW_PLAYBACK_DIAGNOSTICS = "show_playback_diagnostics"
        const val KEY_DEVELOPER_MODE = "developer_mode"
        const val KEY_DISCOVERY_REFRESH_INTERVAL = "discovery_refresh_interval"
    }
}
