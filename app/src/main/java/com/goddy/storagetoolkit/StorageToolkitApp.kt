package com.goddy.storagetoolkit

import android.app.Application
import com.goddy.storagetoolkit.data.database.AppDatabase
import com.goddy.storagetoolkit.repository.ApkRepository
import com.goddy.storagetoolkit.repository.DownloadsRepository
import com.goddy.storagetoolkit.repository.DuplicateRepository
import com.goddy.storagetoolkit.repository.EmptyFolderRepository
import com.goddy.storagetoolkit.repository.LargeFileRepository
import com.goddy.storagetoolkit.repository.ScanHistoryRepository
import com.goddy.storagetoolkit.repository.ZeroByteRepository
import com.goddy.storagetoolkit.scanner.ApkScanner
import com.goddy.storagetoolkit.scanner.DownloadsScanner
import com.goddy.storagetoolkit.scanner.DuplicateScanner
import com.goddy.storagetoolkit.scanner.EmptyFolderScanner
import com.goddy.storagetoolkit.scanner.LargeFileScanner
import com.goddy.storagetoolkit.scanner.ZeroByteScanner
import com.goddy.storagetoolkit.utils.StorageAccessManager

/**
 * Lightweight manual DI container. The app is small enough that a DI framework like
 * Hilt would add build complexity without much benefit; this container wires up
 * singletons once at Application start and hands them to ViewModels via a factory.
 */
class StorageToolkitApp : Application() {

    lateinit var storageAccessManager: StorageAccessManager
        private set
    lateinit var scanHistoryRepository: ScanHistoryRepository
        private set
    lateinit var downloadsRepository: DownloadsRepository
        private set
    lateinit var apkRepository: ApkRepository
        private set
    lateinit var zeroByteRepository: ZeroByteRepository
        private set
    lateinit var emptyFolderRepository: EmptyFolderRepository
        private set
    lateinit var duplicateRepository: DuplicateRepository
        private set
    lateinit var largeFileRepository: LargeFileRepository
        private set

    override fun onCreate() {
        super.onCreate()

        storageAccessManager = StorageAccessManager(this)

        val database = AppDatabase.getInstance(this)
        scanHistoryRepository = ScanHistoryRepository(database.scanHistoryDao())

        downloadsRepository = DownloadsRepository(DownloadsScanner())
        apkRepository = ApkRepository(this, ApkScanner(this))
        zeroByteRepository = ZeroByteRepository(this, ZeroByteScanner())
        emptyFolderRepository = EmptyFolderRepository(this, EmptyFolderScanner())
        duplicateRepository = DuplicateRepository(this, DuplicateScanner(this))
        largeFileRepository = LargeFileRepository(this, LargeFileScanner())
    }
}
