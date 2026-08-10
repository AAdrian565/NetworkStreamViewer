package com.adriant.networkstreamviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adriant.networkstreamviewer.data.settings.AppSettingsRepository
import com.adriant.networkstreamviewer.data.update.GitHubUpdateRepository
import com.adriant.networkstreamviewer.presentation.NdiApp
import com.adriant.networkstreamviewer.ui.theme.NetworkStreamViewerTheme

class MainActivity : ComponentActivity() {
    private val settingsRepository by lazy { AppSettingsRepository(applicationContext) }
    private val updateRepository by lazy { GitHubUpdateRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle()
            NetworkStreamViewerTheme(appTheme = settings.theme) {
                NdiApp(
                    settingsRepository = settingsRepository,
                    updateRepository = updateRepository,
                )
            }
        }
    }
}
