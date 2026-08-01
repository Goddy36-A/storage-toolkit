package com.goddy.storagetoolkit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goddy.storagetoolkit.models.FileCategory
import com.goddy.storagetoolkit.models.FileItem
import com.goddy.storagetoolkit.repository.DownloadsRepository
import com.goddy.storagetoolkit.repository.ScanHistoryRepository
import com.goddy.storagetoolkit.repository.ScanType
import com.goddy.storagetoolkit.utils.StorageAccessManager
import com.goddy.storagetoolkit.utils.StorageRoots
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val hasFolderAccess: Boolean = false,
    val isScanning: Boolean = false,
    val isOrganizing: Boolean = false,
    val filesByCategory: Map<FileCategory, List<FileItem>> = emptyMap(),
    val statusMessage: String? = null
)

class DownloadsViewModel(
    private val storageAccessManager: StorageAccessManager,
    private val downloadsRepository: DownloadsRepository,
    private val scanHistoryRepository: ScanHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

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
            _uiState.value = _uiState.value.copy(isScanning = true, statusMessage = null)
            val files = downloadsRepository.scan(StorageRoots.downloads())
            val grouped = files.groupBy { it.category }
            _uiState.value = _uiState.value.copy(isScanning = false, filesByCategory = grouped)
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    fun organizeAll() {
        val allFiles = _uiState.value.filesByCategory.values.flatten()
        if (allFiles.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOrganizing = true)
            val movedCount = downloadsRepository.organize(StorageRoots.downloads(), allFiles)
            scanHistoryRepository.record(
                scanType = ScanType.DOWNLOADS_ORGANIZER,
                filesFound = allFiles.size,
                filesRemoved = movedCount,
                spaceSavedBytes = 0
            )
            _uiState.value = _uiState.value.copy(
                isOrganizing = false,
                statusMessage = "Organized $movedCount of ${allFiles.size} files"
            )
            scan()
        }
    }
}
