package com.goddy.storagetoolkit.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.goddy.storagetoolkit.data.datastore.ThemeMode
import com.goddy.storagetoolkit.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    var newFolderText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                SectionHeader("Theme")
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ThemeOption("System default", settings.theme == ThemeMode.SYSTEM) { viewModel.setTheme(ThemeMode.SYSTEM) }
                    ThemeOption("Light", settings.theme == ThemeMode.LIGHT) { viewModel.setTheme(ThemeMode.LIGHT) }
                    ThemeOption("Dark", settings.theme == ThemeMode.DARK) { viewModel.setTheme(ThemeMode.DARK) }
                }
            }

            item { Divider(modifier = Modifier.padding(vertical = 12.dp)) }

            item {
                SectionHeader("Recycle Bin")
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "Auto-delete after ${settings.autoDeleteRecycleBinDays} day(s)",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "Applies once the Recycle Bin feature ships — saved now so it's ready.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = settings.autoDeleteRecycleBinDays.toFloat(),
                        onValueChange = { viewModel.setAutoDeleteDays(it.toInt()) },
                        valueRange = 1f..90f,
                        steps = 88
                    )
                }
            }

            item { Divider(modifier = Modifier.padding(vertical = 12.dp)) }

            item {
                SectionHeader("Ignored Folders")
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "Folders listed here are skipped by every scanner, in addition to hidden folders and Android/.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newFolderText,
                            onValueChange = { newFolderText = it },
                            label = { Text("Folder name, e.g. WhatsApp") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (newFolderText.isNotBlank()) {
                                    viewModel.addIgnoredFolder(newFolderText)
                                    newFolderText = ""
                                }
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("Add")
                        }
                    }
                }
            }

            items(settings.ignoredFolders.sorted()) { folder ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(folder, style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { viewModel.removeIgnoredFolder(folder) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove $folder")
                    }
                }
            }

            item { Divider(modifier = Modifier.padding(vertical = 12.dp)) }

            item {
                SectionHeader("Language")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("English", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "More languages coming soon",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { Divider(modifier = Modifier.padding(vertical = 12.dp)) }

            item {
                SettingsNavRow(title = "About", onClick = onOpenAbout)
                SettingsNavRow(title = "Privacy Policy", onClick = onOpenPrivacyPolicy)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SettingsNavRow(title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}
