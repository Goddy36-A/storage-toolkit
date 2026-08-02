package com.goddy.storagetoolkit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goddy.storagetoolkit.data.datastore.SettingsManager
import com.goddy.storagetoolkit.models.CategoryBreakdown
import com.goddy.storagetoolkit.repository.StorageAnalyzerRepository
import com.goddy.storagetoolkit.utils.StorageAccessManager
import com.goddy.storagetoolkit.utils.StorageRoots
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StorageAnalyzerUiState(
    val hasFolderAccess: Boolean = false,
    val isScanning: Boolean = false,
    val breakdown: List<CategoryBreakdown> = emptyList()
) {
    val totalBytes: Long get() = breakdown.sumOf { it.totalSizeBytes }
}

class StorageAnalyzerViewModel(
    private val storageAccessManager: StorageAccessManager,
    private val storageAnalyzerRepository: StorageAnalyzerRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageAnalyzerUiState())
    val uiState: StateFlow<StorageAnalyzerUiState> = _uiState.asStateFlow()

    val needsLegacyPermission: Boolean get() = storageAccessManager.needsLegacyFlow
    val legacyPermission: String get() = storageAccessManager.legacyPermission
    fun requestIntent() = storageAccessManager.buildRequestIntent()

    private var scanJob: Job? = null

    init {
        viewModelScope.launch {
            storageAccessManager.grantedFlow.collect { granted ->
                _uiState.value = _uiState.value.copy(hasFolderAccess = granted)
                if (granted) scan()
            }
        }
    }

    fun refreshAccess() {
        storageAccessManager.refresh()
    }

    fun scan() {
        if (!_uiState.value.hasFolderAccess) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            val breakdown = storageAnalyzerRepository.scan(
                StorageRoots.primary(),
                settingsManager.currentIgnoredFolders()
            )
            _uiState.value = _uiState.value.copy(isScanning = false, breakdown = breakdown)
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _uiState.value = _uiState.value.copy(isScanning = false)
    }
}
