package com.goddy.storagetoolkit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goddy.storagetoolkit.models.FileItem
import com.goddy.storagetoolkit.repository.ScanHistoryRepository
import com.goddy.storagetoolkit.repository.ScanType
import com.goddy.storagetoolkit.repository.ZeroByteRepository
import com.goddy.storagetoolkit.utils.StorageAccessManager
import com.goddy.storagetoolkit.utils.StorageRoots
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ZeroByteUiState(
    val hasFolderAccess: Boolean = false,
    val isScanning: Boolean = false,
    val files: List<FileItem> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val statusMessage: String? = null
)

class ZeroByteViewModel(
    private val storageAccessManager: StorageAccessManager,
    private val zeroByteRepository: ZeroByteRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val settingsManager: com.goddy.storagetoolkit.data.datastore.SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ZeroByteUiState())
    val uiState: StateFlow<ZeroByteUiState> = _uiState.asStateFlow()

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

    /** Call from onResume — the grant happens in system Settings, not in-app. */
    fun refreshAccess() {
        storageAccessManager.refresh()
    }

    fun scan() {
        if (!_uiState.value.hasFolderAccess) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, statusMessage = null)
            val files = zeroByteRepository.scan(StorageRoots.primary(), settingsManager.currentIgnoredFolders())
            scanHistoryRepository.record(
                scanType = ScanType.ZERO_BYTE,
                filesFound = files.size,
                filesRemoved = 0,
                spaceSavedBytes = 0
            )
            _uiState.value = _uiState.value.copy(isScanning = false, files = files, selectedIds = emptySet())
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    fun toggleSelection(documentId: String) {
        val current = _uiState.value.selectedIds
        val updated = if (documentId in current) current - documentId else current + documentId
        _uiState.value = _uiState.value.copy(selectedIds = updated)
    }

    fun selectAll() {
        _uiState.value = _uiState.value.copy(selectedIds = _uiState.value.files.map { it.documentId }.toSet())
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedIds = emptySet())
    }

    fun deleteSelected() {
        val selected = _uiState.value.files.filter { it.documentId in _uiState.value.selectedIds }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            val foundBeforeDelete = _uiState.value.files.size
            val deletedCount = zeroByteRepository.delete(selected)
            scanHistoryRepository.record(
                scanType = ScanType.ZERO_BYTE,
                filesFound = foundBeforeDelete,
                filesRemoved = deletedCount,
                spaceSavedBytes = 0
            )
            _uiState.value = _uiState.value.copy(statusMessage = "Deleted $deletedCount file(s)")
            scan()
        }
    }
}
