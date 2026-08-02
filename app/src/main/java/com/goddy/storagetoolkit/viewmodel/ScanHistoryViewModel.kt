package com.goddy.storagetoolkit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goddy.storagetoolkit.data.database.entities.ScanHistoryEntity
import com.goddy.storagetoolkit.repository.ScanHistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScanHistoryViewModel(
    private val scanHistoryRepository: ScanHistoryRepository
) : ViewModel() {

    val history: StateFlow<List<ScanHistoryEntity>> = scanHistoryRepository.history.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun clearHistory() {
        viewModelScope.launch { scanHistoryRepository.clearHistory() }
    }
}
