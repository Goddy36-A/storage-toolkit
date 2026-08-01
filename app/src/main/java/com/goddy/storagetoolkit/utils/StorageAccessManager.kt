package com.goddy.storagetoolkit.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wraps Android's "All Files Access" special permission (Android 11+) and its
 * legacy equivalent, WRITE_EXTERNAL_STORAGE (Android 10 and below). One grant here
 * is shared by every scanner in the app — there is deliberately no per-feature
 * folder picker anymore.
 *
 * This is a considered scope decision, not an oversight: Storage Toolkit's entire
 * purpose is managing files across shared storage, which is exactly the "core
 * functionality" carve-out Play Store policy allows for MANAGE_EXTERNAL_STORAGE.
 * A generic utility app bolting this on for a minor feature would not qualify.
 *
 * The grant happens in system Settings, not in-app, so [refresh] must be called
 * when the app resumes to notice a change — see [com.goddy.storagetoolkit.ui.common.OnResumeEffect].
 */
class StorageAccessManager(private val context: Context) {

    private val _grantedFlow = MutableStateFlow(checkGranted())
    val grantedFlow: StateFlow<Boolean> = _grantedFlow.asStateFlow()

    val needsLegacyFlow: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.R
    val legacyPermission: String = Manifest.permission.WRITE_EXTERNAL_STORAGE

    fun refresh() {
        _grantedFlow.value = checkGranted()
    }

    private fun checkGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, legacyPermission) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    /** Android 11+: opens this app's All Files Access settings screen, with a fallback
     *  for OEM builds where the app-specific action isn't resolvable. */
    fun buildRequestIntent(): Intent {
        return try {
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        } catch (e: Exception) {
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        }
    }
}
