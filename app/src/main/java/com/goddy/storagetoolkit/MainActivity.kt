package com.goddy.storagetoolkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.goddy.storagetoolkit.data.datastore.ThemeMode
import com.goddy.storagetoolkit.navigation.AppNavGraph
import com.goddy.storagetoolkit.ui.theme.StorageToolkitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as StorageToolkitApp
            val settings by app.settingsManager.settingsFlow.collectAsState(
                initial = com.goddy.storagetoolkit.data.datastore.AppSettings()
            )
            val useDarkTheme = when (settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            StorageToolkitTheme(darkTheme = useDarkTheme) {
                AppNavGraph(app = app)
            }
        }
    }
}
