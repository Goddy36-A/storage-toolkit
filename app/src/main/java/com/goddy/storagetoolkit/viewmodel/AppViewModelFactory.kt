package com.goddy.storagetoolkit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.goddy.storagetoolkit.StorageToolkitApp

class AppViewModelFactory(private val app: StorageToolkitApp) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when (modelClass) {
            DashboardViewModel::class.java ->
                DashboardViewModel(app.scanHistoryRepository) as T

            DownloadsViewModel::class.java ->
                DownloadsViewModel(app.storageAccessManager, app.downloadsRepository, app.scanHistoryRepository) as T

            ApkManagerViewModel::class.java ->
                ApkManagerViewModel(app.storageAccessManager, app.apkRepository, app.scanHistoryRepository, app.settingsManager) as T

            ZeroByteViewModel::class.java ->
                ZeroByteViewModel(app.storageAccessManager, app.zeroByteRepository, app.scanHistoryRepository, app.settingsManager) as T

            EmptyFolderViewModel::class.java ->
                EmptyFolderViewModel(app.storageAccessManager, app.emptyFolderRepository, app.scanHistoryRepository, app.settingsManager) as T

            DuplicateViewModel::class.java ->
                DuplicateViewModel(app.storageAccessManager, app.duplicateRepository, app.scanHistoryRepository, app.settingsManager) as T

            LargeFileViewModel::class.java ->
                LargeFileViewModel(app.storageAccessManager, app.largeFileRepository, app.scanHistoryRepository, app.settingsManager) as T

            SettingsViewModel::class.java ->
                SettingsViewModel(app.settingsManager) as T

            RecycleBinViewModel::class.java ->
                RecycleBinViewModel(app.recycleBinRepository, app.settingsManager) as T

            StorageAnalyzerViewModel::class.java ->
                StorageAnalyzerViewModel(app.storageAccessManager, app.storageAnalyzerRepository, app.settingsManager) as T

            SearchViewModel::class.java ->
                SearchViewModel(app.storageAccessManager, app.searchRepository, app.settingsManager) as T

            ScanHistoryViewModel::class.java ->
                ScanHistoryViewModel(app.scanHistoryRepository) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
