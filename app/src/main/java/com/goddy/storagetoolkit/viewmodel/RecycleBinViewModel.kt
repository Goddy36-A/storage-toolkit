package com.goddy.storagetoolkit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goddy.storagetoolkit.data.database.entities.RecycleBinEntity
import com.goddy.storagetoolkit.data.datastore.SettingsManager
import com.goddy.storagetoolkit.repository.RecycleBinRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class RecycleBinUiState(
    val items: List<RecycleBinEntity> = emptyList(),
    val autoDeleteDays: Int = 30,
    val statusMessage: String? = null
)

class RecycleBinViewModel(
    private val recycleBinRepository: RecycleBinRepository,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecycleBinUiState())
    val uiState: StateFlow<RecycleBinUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            recycleBinRepository.itemsFlow.collect { items ->
                _uiState.value = _uiState.value.copy(items = items)
            }
        }
        viewModelScope.launch {
            settingsManager.settingsFlow.collect { settings ->
                _uiState.value = _uiState.value.copy(autoDeleteDays = settings.autoDeleteRecycleBinDays)
            }
        }
        // Opportunistic purge: there's no background worker, so "auto-delete after X
        // days" is enforced whenever the user actually opens the Recycle Bin.
        viewModelScope.launch {
            val days = settingsManager.settingsFlow.first().autoDeleteRecycleBinDays
            recycleBinRepository.purgeOlderThan(days)
        }
    }

    fun restore(entry: RecycleBinEntity) {
        viewModelScope.launch {
            val restored = recycleBinRepository.restore(entry)
            val message = if (restored) {
                "Restored '${entry.originalName}'"
            } else {
                "Couldn't restore '${entry.originalName}' -- its original folder may be gone"
            }
            _uiState.value = _uiState.value.copy(statusMessage = message)
        }
    }

    fun deleteForever(entry: RecycleBinEntity) {
        viewModelScope.launch {
            recycleBinRepository.permanentlyDelete(entry)
            _uiState.value = _uiState.value.copy(statusMessage = "Deleted '${entry.originalName}' permanently")
        }
    }

    fun emptyBin() {
        viewModelScope.launch {
            val current = _uiState.value.items
            for (entry in current) recycleBinRepository.permanentlyDelete(entry)
            _uiState.value = _uiState.value.copy(statusMessage = "Recycle bin emptied")
        }
    }
}
