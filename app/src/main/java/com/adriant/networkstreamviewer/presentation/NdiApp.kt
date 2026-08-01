package com.adriant.networkstreamviewer.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adriant.networkstreamviewer.data.ndi.NdiPlayerController
import com.adriant.networkstreamviewer.presentation.player.PlayerScreen
import com.adriant.networkstreamviewer.presentation.sources.SourceListScreen

@Composable
fun NdiApp(
    viewModel: NdiViewModel = viewModel(factory = NdiViewModel.Factory())
) {
    val permission = rememberLocalNetworkPermissionState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerController = remember { NdiPlayerController() }
    val selectedSource = uiState.selectedSource

    LocalNetworkMulticastEffect(enabled = permission.isGranted)
    ScreenOrientationEffect(showingPlayer = selectedSource != null)

    LaunchedEffect(permission.isGranted) {
        if (permission.isGranted) viewModel.refreshSources() else viewModel.stopDiscovery()
    }

    selectedSource?.let { source ->
        BackHandler { viewModel.clearSelectedSource() }
        PlayerScreen(
            source = source,
            playerController = playerController,
            onBack = viewModel::clearSelectedSource
        )
        return
    }

    SourceListScreen(
        sources = uiState.sources,
        status = if (permission.isGranted) {
            uiState.statusMessage
        } else {
            "Local-network access is required to find NDI® sources."
        },
        permissionGranted = permission.isGranted,
        isRefreshing = uiState.isRefreshing,
        onRefresh = viewModel::refreshSources,
        onRequestPermission = permission.request,
        onSourceSelected = viewModel::selectSource
    )
}
