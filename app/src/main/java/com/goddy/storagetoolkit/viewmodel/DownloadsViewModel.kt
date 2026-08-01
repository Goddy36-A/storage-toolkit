package com.goddy.storagetoolkit.viewmodel

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goddy.storagetoolkit.models.FileCategory
import com.goddy.storagetoolkit.models.FileItem
import com.goddy.storagetoolkit.repository.DownloadsRepository
import com.goddy.storagetoolkit.repository.ScanHistoryRepository
import com.goddy.storagetoolkit.repository.ScanType
import com.goddy.storagetoolkit.utils.SafManager
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
    private val safManager: SafManager,
    private val downloadsRepository: DownloadsRepository,
    private val scanHistoryRepository: ScanHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private var treeUriString: String? = null
    private var scanJob: Job? = null

    init {
        viewModelScope.launch {
            safManager.treeUriFlow.collect { uri ->
                treeUriString = uri
                _uiState.value = _uiState.value.copy(hasFolderAccess = uri != null)
                if (uri != null) scan()
            }
        }
    }

    fun onFolderGranted(uri: Uri) {
        viewModelScope.launch {
            safManager.persistTreeUri(uri)
        }
    }

    fun scan() {
        val uriString = treeUriString ?: return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, statusMessage = null)
            val root = safManager.getDocumentTree(uriString)
            if (root == null) {
                _uiState.value = _uiState.value.copy(isScanning = false, statusMessage = "Could not open folder")
                return@launch
            }
            val files = downloadsRepository.scan(root)
            val grouped = files.groupBy { it.category }
            _uiState.value = _uiState.value.copy(isScanning = false, filesByCategory = grouped)
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    fun organizeAll() {
        val uriString = treeUriString ?: return
        val allFiles = _uiState.value.filesByCategory.values.flatten()
        if (allFiles.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOrganizing = true)
            val root = safManager.getDocumentTree(uriString)
            if (root == null) {
                _uiState.value = _uiState.value.copy(isOrganizing = false, statusMessage = "Could not open folder")
                return@launch
            }
            val movedCount = downloadsRepository.organize(root, allFiles)
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
