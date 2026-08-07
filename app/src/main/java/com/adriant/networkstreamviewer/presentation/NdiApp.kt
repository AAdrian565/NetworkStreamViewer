package com.adriant.networkstreamviewer.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.adriant.networkstreamviewer.data.settings.AppSettingsRepository
import com.adriant.networkstreamviewer.domain.repository.UpdateRepository
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adriant.networkstreamviewer.data.ndi.NdiPlayerController
import com.adriant.networkstreamviewer.presentation.camera.CameraSenderScreen
import com.adriant.networkstreamviewer.presentation.player.PlayerScreen
import com.adriant.networkstreamviewer.presentation.sources.SourceListScreen
import com.adriant.networkstreamviewer.presentation.settings.AboutScreen
import com.adriant.networkstreamviewer.presentation.settings.SettingsScreen
import kotlinx.coroutines.delay

@Composable
fun NdiApp(
    settingsRepository: AppSettingsRepository,
    updateRepository: UpdateRepository,
    viewModel: NdiViewModel = viewModel(
        factory = NdiViewModel.Factory(settingsRepository, updateRepository)
    )
) {
    val permission = rememberLocalNetworkPermissionState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerController = remember { NdiPlayerController() }
    val selectedSource = uiState.selectedSource

    DisposableEffect(playerController) {
        onDispose { playerController.close() }
    }

    LocalNetworkMulticastEffect(enabled = permission.isGranted)
    ScreenOrientationEffect(showingPlayer = selectedSource != null)

    LaunchedEffect(permission.isGranted) {
        if (permission.isGranted) viewModel.refreshSources() else viewModel.stopDiscovery()
    }

    LaunchedEffect(
        permission.isGranted,
        uiState.isSettingsOpen,
        uiState.isCameraSenderOpen,
        selectedSource,
        uiState.settings.discoveryRefreshInterval
    ) {
        val intervalMillis = uiState.settings.discoveryRefreshInterval.intervalMillis
        if (permission.isGranted &&
            !uiState.isSettingsOpen &&
            !uiState.isCameraSenderOpen &&
            selectedSource == null &&
            intervalMillis != null
        ) {
            while (true) {
                delay(intervalMillis)
                viewModel.refreshSources()
            }
        }
    }

    selectedSource?.let { source ->
        BackHandler { viewModel.clearSelectedSource() }
        PlayerScreen(
            source = source,
            playerController = playerController,
            defaultBandwidth = uiState.settings.defaultBandwidth,
            keepScreenAwake = uiState.settings.keepScreenAwake,
            showDiagnostics = uiState.settings.showPlaybackDiagnostics,
            onBack = viewModel::clearSelectedSource
        )
        return
    }


    if (uiState.isAboutOpen) {
        BackHandler(onBack = viewModel::closeAbout)
        AboutScreen(
            developerOptionsUnlocked = uiState.areDeveloperOptionsUnlocked,
            updateState = uiState.update,
            onUnlockDeveloperOptions = viewModel::unlockDeveloperOptions,
            onCheckForUpdates = viewModel::checkForUpdates,
            onDownloadUpdate = viewModel::downloadUpdate,
            onBack = viewModel::closeAbout
        )
        return
    }

    if (uiState.isSettingsOpen) {
        val closeSettings = {
            viewModel.closeSettings()
            if (permission.isGranted) viewModel.refreshSources()
        }
        BackHandler(onBack = closeSettings)
        SettingsScreen(
            settings = uiState.settings,
            developerOptionsUnlocked = uiState.areDeveloperOptionsUnlocked,
            onThemeChanged = viewModel::setTheme,
            onDefaultBandwidthChanged = viewModel::setDefaultBandwidth,
            onKeepScreenAwakeChanged = viewModel::setKeepScreenAwake,
            onShowPlaybackDiagnosticsChanged = viewModel::setShowPlaybackDiagnostics,
            onDeveloperModeChanged = viewModel::setDeveloperMode,
            onDiscoveryRefreshIntervalChanged = viewModel::setDiscoveryRefreshInterval,
            onOpenAbout = viewModel::openAbout,
            onBack = closeSettings
        )
        return
    }

    if (uiState.isCameraSenderOpen) {
        val closeCameraSender = {
            viewModel.closeCameraSender()
            if (permission.isGranted) viewModel.refreshSources()
        }
        BackHandler(onBack = closeCameraSender)
        CameraSenderScreen(
            localNetworkPermissionGranted = permission.isGranted,
            onRequestLocalNetworkPermission = permission.request,
            onBack = closeCameraSender
        )
        return
    }

    SourceListScreen(
        sources = uiState.displayedSources,
        status = if (permission.isGranted) {
            uiState.statusMessage
        } else {
            "Local-network access is required to find NDI® sources."
        },
        permissionGranted = permission.isGranted,
        isRefreshing = uiState.isRefreshing,
        onRefresh = viewModel::refreshSources,
        onRequestPermission = permission.request,
        onOpenCameraSender = viewModel::openCameraSender,
        onOpenSettings = viewModel::openSettings,
        onSourceSelected = viewModel::selectSource
    )
}
