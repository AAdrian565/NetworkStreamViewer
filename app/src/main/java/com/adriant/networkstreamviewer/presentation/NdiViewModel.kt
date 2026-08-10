package com.adriant.networkstreamviewer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.adriant.networkstreamviewer.BuildConfig
import com.adriant.networkstreamviewer.data.ndi.NdiSourceRepositoryImpl
import com.adriant.networkstreamviewer.data.settings.AppSettingsRepository
import com.adriant.networkstreamviewer.domain.model.AppTheme
import com.adriant.networkstreamviewer.domain.model.DiscoveryRefreshInterval
import com.adriant.networkstreamviewer.domain.model.NdiBandwidth
import com.adriant.networkstreamviewer.domain.model.NdiSource
import com.adriant.networkstreamviewer.domain.repository.NdiSourceRepository
import com.adriant.networkstreamviewer.domain.repository.UpdateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class NdiViewModel(
    private val repository: NdiSourceRepository,
    private val settingsRepository: AppSettingsRepository,
    private val updateRepository: UpdateRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(NdiUiState())
    val uiState: StateFlow<NdiUiState> = mutableUiState.asStateFlow()

    private var discoveryJob: Job? = null
    private var detailsJob: Job? = null
    private var updateJob: Job? = null
    private var refreshGeneration = 0
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                mutableUiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun refreshSources() {
        discoveryJob?.cancel()
        detailsJob?.cancel()
        val generation = ++refreshGeneration

        discoveryJob =
            viewModelScope.launch {
                mutableUiState.update { it.copy(isRefreshing = true, errorMessage = null) }
                try {
                    if (!mutableUiState.value.isInitialized && !repository.initialize()) {
                        mutableUiState.update {
                            it.copy(errorMessage = "The NDI runtime could not be initialized.")
                        }
                        return@launch
                    }

                    mutableUiState.update { it.copy(isInitialized = true) }
                    repeat(DISCOVERY_ATTEMPTS) {
                        val sources = repository.discoverSources(DISCOVERY_TIMEOUT_MS)
                        mutableUiState.update { it.copy(sources = sources, errorMessage = null) }
                        if (sources.isNotEmpty()) {
                            loadSourceDetails(sources, generation)
                            return@launch
                        }
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    mutableUiState.update {
                        it.copy(errorMessage = "NDI discovery failed. Check the network and try again.")
                    }
                } finally {
                    if (generation == refreshGeneration) {
                        mutableUiState.update {
                            it.copy(isRefreshing = false, hasCompletedRefresh = true)
                        }
                    }
                }
            }
    }

    fun stopDiscovery() {
        refreshGeneration++
        discoveryJob?.cancel()
        discoveryJob = null
        detailsJob?.cancel()
        detailsJob = null
        mutableUiState.update { it.copy(isRefreshing = false) }
    }

    private fun loadSourceDetails(
        sources: List<NdiSource>,
        generation: Int,
    ) {
        detailsJob?.cancel()
        detailsJob =
            viewModelScope.launch {
                val slots = Semaphore(MAX_CONCURRENT_PROBES)
                supervisorScope {
                    sources
                        .map { source ->
                            async {
                                val details =
                                    try {
                                        slots.withPermit { repository.probeSource(source) }
                                    } catch (cancellation: CancellationException) {
                                        throw cancellation
                                    } catch (_: Exception) {
                                        null
                                    }
                                if (generation == refreshGeneration) {
                                    mutableUiState.update { state ->
                                        state.copy(
                                            sources =
                                                state.sources.map { current ->
                                                    if (current.name == source.name && current.url == source.url) {
                                                        current.copy(details = details, isLoadingDetails = false)
                                                    } else {
                                                        current
                                                    }
                                                },
                                        )
                                    }
                                }
                            }
                        }.awaitAll()
                }
            }
    }

    fun selectSource(source: NdiSource) {
        mutableUiState.update { it.copy(selectedSource = source) }
    }

    fun clearSelectedSource() {
        mutableUiState.update { it.copy(selectedSource = null) }
    }

    fun openCameraSender() {
        stopDiscovery()
        mutableUiState.update {
            it.copy(isCameraSenderOpen = true, selectedSource = null)
        }
    }

    fun closeCameraSender() {
        mutableUiState.update { it.copy(isCameraSenderOpen = false) }
    }

    fun openSettings() {
        stopDiscovery()
        mutableUiState.update {
            it.copy(isSettingsOpen = true, isAboutOpen = false, selectedSource = null)
        }
    }

    fun closeSettings() {
        mutableUiState.update { it.copy(isSettingsOpen = false, isAboutOpen = false) }
    }

    fun openAbout() {
        mutableUiState.update { it.copy(isAboutOpen = true) }
    }

    fun closeAbout() {
        mutableUiState.update { it.copy(isAboutOpen = false) }
    }

    fun checkForUpdates() {
        updateJob?.cancel()
        updateJob =
            viewModelScope.launch {
                mutableUiState.update {
                    it.copy(update = UpdateUiState(status = UpdateStatus.CHECKING))
                }
                try {
                    val update = updateRepository.findLatestUpdate(BuildConfig.VERSION_NAME)
                    mutableUiState.update {
                        it.copy(
                            update =
                                UpdateUiState(
                                    status =
                                        if (update == null) {
                                            UpdateStatus.UP_TO_DATE
                                        } else {
                                            UpdateStatus.AVAILABLE
                                        },
                                    update = update,
                                ),
                        )
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    mutableUiState.update {
                        it.copy(
                            update =
                                UpdateUiState(
                                    status = UpdateStatus.ERROR,
                                    errorMessage = "Could not check for updates.",
                                ),
                        )
                    }
                }
            }
    }

    fun downloadUpdate() {
        val update = mutableUiState.value.update.update ?: return
        updateJob?.cancel()
        updateJob =
            viewModelScope.launch {
                mutableUiState.update {
                    it.copy(update = it.update.copy(status = UpdateStatus.DOWNLOADING, errorMessage = null))
                }
                try {
                    val path = updateRepository.downloadUpdate(update)
                    mutableUiState.update {
                        it.copy(
                            update =
                                it.update.copy(
                                    status = UpdateStatus.READY,
                                    downloadedApkPath = path,
                                    errorMessage = null,
                                ),
                        )
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    mutableUiState.update {
                        it.copy(
                            update =
                                it.update.copy(
                                    status = UpdateStatus.AVAILABLE,
                                    errorMessage = "Could not download the update.",
                                ),
                        )
                    }
                }
            }
    }

    fun setTheme(theme: AppTheme) = settingsRepository.setTheme(theme)

    fun setDefaultBandwidth(bandwidth: NdiBandwidth) = settingsRepository.setDefaultBandwidth(bandwidth)

    fun setKeepScreenAwake(enabled: Boolean) = settingsRepository.setKeepScreenAwake(enabled)

    fun setShowPlaybackDiagnostics(enabled: Boolean) = settingsRepository.setShowPlaybackDiagnostics(enabled)

    fun setDeveloperMode(enabled: Boolean) = settingsRepository.setDeveloperMode(enabled)

    fun unlockDeveloperOptions() {
        mutableUiState.update { it.copy(areDeveloperOptionsUnlocked = true) }
    }

    fun setDiscoveryRefreshInterval(interval: DiscoveryRefreshInterval) =
        settingsRepository.setDiscoveryRefreshInterval(interval)

    override fun onCleared() {
        stopDiscovery()
        cleanupScope
            .launch {
                repository.shutdown()
            }.invokeOnCompletion {
                cleanupScope.cancel()
            }
        super.onCleared()
    }

    class Factory(
        private val settingsRepository: AppSettingsRepository,
        private val updateRepository: UpdateRepository,
        private val repository: NdiSourceRepository = NdiSourceRepositoryImpl(),
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NdiViewModel::class.java))
            return NdiViewModel(repository, settingsRepository, updateRepository) as T
        }
    }

    private companion object {
        const val DISCOVERY_TIMEOUT_MS = 1_000
        const val DISCOVERY_ATTEMPTS = 5
        const val MAX_CONCURRENT_PROBES = 3
    }
}
