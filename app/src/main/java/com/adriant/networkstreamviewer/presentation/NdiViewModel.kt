package com.adriant.networkstreamviewer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.adriant.networkstreamviewer.data.ndi.NdiSourceRepositoryImpl
import com.adriant.networkstreamviewer.domain.model.NdiSource
import com.adriant.networkstreamviewer.domain.repository.NdiSourceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NdiViewModel(
    private val repository: NdiSourceRepository
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(NdiUiState())
    val uiState: StateFlow<NdiUiState> = mutableUiState.asStateFlow()

    private var discoveryJob: Job? = null
    private var refreshGeneration = 0

    fun refreshSources() {
        discoveryJob?.cancel()
        val generation = ++refreshGeneration

        discoveryJob = viewModelScope.launch {
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
                    if (sources.isNotEmpty()) return@launch
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
        mutableUiState.update { it.copy(isRefreshing = false) }
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

    override fun onCleared() {
        stopDiscovery()
        repository.shutdown()
    }

    class Factory(
        private val repository: NdiSourceRepository = NdiSourceRepositoryImpl()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(NdiViewModel::class.java))
            return NdiViewModel(repository) as T
        }
    }

    private companion object {
        const val DISCOVERY_TIMEOUT_MS = 1_000
        const val DISCOVERY_ATTEMPTS = 5
    }
}
