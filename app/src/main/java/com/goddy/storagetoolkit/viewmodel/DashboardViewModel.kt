package com.goddy.storagetoolkit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goddy.storagetoolkit.models.StorageStats
import com.goddy.storagetoolkit.repository.ScanHistoryRepository
import com.goddy.storagetoolkit.repository.ScanType
import com.goddy.storagetoolkit.utils.StorageStatsUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val storageStats: StorageStats = StorageStats(0, 0, 0),
    val downloadsLastScan: Long? = null,
    val apkLastScan: Long? = null,
    val zeroByteLastScan: Long? = null,
    val emptyFolderLastScan: Long? = null,
    val duplicateLastScan: Long? = null
)

class DashboardViewModel(
    private val scanHistoryRepository: ScanHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refreshDeviceStats()
        loadLastScanTimes()
    }

    fun refreshDeviceStats() {
        _uiState.value = _uiState.value.copy(storageStats = StorageStatsUtil.readDeviceStorageStats())
    }

    private fun loadLastScanTimes() {
        viewModelScope.launch {
            val downloadsTime = scanHistoryRepository.lastScanTime(ScanType.DOWNLOADS_ORGANIZER)
            val apkTime = scanHistoryRepository.lastScanTime(ScanType.APK_MANAGER)
            val zeroByteTime = scanHistoryRepository.lastScanTime(ScanType.ZERO_BYTE)
            val emptyFolderTime = scanHistoryRepository.lastScanTime(ScanType.EMPTY_FOLDER)
            val duplicateTime = scanHistoryRepository.lastScanTime(ScanType.DUPLICATE_FILES)
            _uiState.value = _uiState.value.copy(
                downloadsLastScan = downloadsTime,
                apkLastScan = apkTime,
                zeroByteLastScan = zeroByteTime,
                emptyFolderLastScan = emptyFolderTime,
                duplicateLastScan = duplicateTime
            )
        }
    }
}
