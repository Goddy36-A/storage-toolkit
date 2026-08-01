package com.goddy.storagetoolkit.utils

import android.os.Environment
import android.os.StatFs
import com.goddy.storagetoolkit.models.StorageStats

object StorageStatsUtil {

    /** Reads total/used/free space on the primary shared storage volume. No permission required. */
    fun readDeviceStorageStats(): StorageStats {
        val path = Environment.getExternalStorageDirectory()
        val statFs = StatFs(path.path)
        val total = statFs.blockCountLong * statFs.blockSizeLong
        val free = statFs.availableBlocksLong * statFs.blockSizeLong
        val used = total - free
        return StorageStats(totalBytes = total, usedBytes = used, freeBytes = free)
    }
}
