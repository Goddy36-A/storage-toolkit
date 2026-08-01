package com.goddy.storagetoolkit

import android.app.Application
import com.goddy.storagetoolkit.data.database.AppDatabase
import com.goddy.storagetoolkit.data.datastore.PreferencesManager
import com.goddy.storagetoolkit.repository.ApkRepository
import com.goddy.storagetoolkit.repository.DownloadsRepository
import com.goddy.storagetoolkit.repository.ScanHistoryRepository
import com.goddy.storagetoolkit.scanner.ApkScanner
import com.goddy.storagetoolkit.scanner.DownloadsScanner
import com.goddy.storagetoolkit.utils.SafManager

/**
 * Lightweight manual DI container. The app is small enough that a DI framework like
 * Hilt would add build complexity without much benefit; this container wires up
 * singletons once at Application start and hands them to ViewModels via a factory.
 */
class StorageToolkitApp : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set
    lateinit var safManager: SafManager
        private set
    lateinit var scanHistoryRepository: ScanHistoryRepository
        private set
    lateinit var downloadsRepository: DownloadsRepository
        private set
    lateinit var apkRepository: ApkRepository
        private set

    override fun onCreate() {
        super.onCreate()

        preferencesManager = PreferencesManager(this)
        safManager = SafManager(this, preferencesManager)

        val database = AppDatabase.getInstance(this)
        scanHistoryRepository = ScanHistoryRepository(database.scanHistoryDao())

        downloadsRepository = DownloadsRepository(this, DownloadsScanner())
        apkRepository = ApkRepository(this, ApkScanner(this))
    }
}
