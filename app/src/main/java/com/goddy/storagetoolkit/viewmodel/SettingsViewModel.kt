package com.goddy.storagetoolkit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goddy.storagetoolkit.data.datastore.AppSettings
import com.goddy.storagetoolkit.data.datastore.SettingsManager
import com.goddy.storagetoolkit.data.datastore.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsManager: SettingsManager) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsManager.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch { settingsManager.setTheme(theme) }
    }

    fun setAutoDeleteDays(days: Int) {
        viewModelScope.launch { settingsManager.setAutoDeleteDays(days) }
    }

    fun addIgnoredFolder(name: String) {
        viewModelScope.launch { settingsManager.addIgnoredFolder(name) }
    }

    fun removeIgnoredFolder(name: String) {
        viewModelScope.launch { settingsManager.removeIgnoredFolder(name) }
    }
}
