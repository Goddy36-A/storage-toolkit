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
                DownloadsViewModel(app.safManager, app.downloadsRepository, app.scanHistoryRepository) as T

            ApkManagerViewModel::class.java ->
                ApkManagerViewModel(app.safManager, app.apkRepository, app.scanHistoryRepository) as T

            ZeroByteViewModel::class.java ->
                ZeroByteViewModel(app.safManager, app.zeroByteRepository, app.scanHistoryRepository) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
