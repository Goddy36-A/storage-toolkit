package com.goddy.storagetoolkit.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            PolicySection(
                title = "What this app accesses",
                body = "Storage Toolkit requests All Files Access (Android 11+) or the " +
                    "legacy storage permission (Android 10 and below) so it can scan and " +
                    "manage files across your device's shared storage. This is required " +
                    "for its core purpose: finding duplicate, zero-byte, large, and stray " +
                    "APK files, and organizing your Downloads folder."
            )
            PolicySection(
                title = "What never leaves your device",
                body = "All scanning, hashing, and file operations happen entirely on-device. " +
                    "This app has no server, no analytics SDK, and no network calls of any " +
                    "kind. File names, contents, hashes, and scan results are never " +
                    "transmitted anywhere."
            )
            PolicySection(
                title = "What's stored, and where",
                body = "Scan history (what was found, what was removed, when) and your " +
                    "Settings (theme, ignored folders, auto-delete preference) are stored " +
                    "locally on your device only, using Room and DataStore. Uninstalling " +
                    "the app removes this data along with it."
            )
            PolicySection(
                title = "What this app does not do",
                body = "No RAM boosting, CPU cooling, or battery \"optimization\" — these are " +
                    "not real Android capabilities and this app doesn't pretend otherwise. " +
                    "No ads, no third-party trackers, no data sale of any kind, because " +
                    "there is no data collection to sell."
            )
            PolicySection(
                title = "Deletion and the Recycle Bin",
                body = "Deleting from Duplicate, Zero-byte, Large File, or APK Manager moves the " +
                    "file to this app's private Recycle Bin instead of removing it immediately -- " +
                    "you can restore it from Settings > Recycle Bin, or let it auto-delete after " +
                    "your configured retention period. Empty Folder Cleaner deletes immediately, " +
                    "without a Recycle Bin step, since an empty folder has no content to lose."
            )
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp)
    )
    Text(body, style = MaterialTheme.typography.bodyMedium)
}
