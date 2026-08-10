package com.adriant.networkstreamviewer.presentation

import com.adriant.networkstreamviewer.domain.model.AppUpdate

enum class UpdateStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    AVAILABLE,
    DOWNLOADING,
    READY,
    ERROR,
}

data class UpdateUiState(
    val status: UpdateStatus = UpdateStatus.IDLE,
    val update: AppUpdate? = null,
    val downloadedApkPath: String? = null,
    val errorMessage: String? = null,
)
