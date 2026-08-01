package com.goddy.storagetoolkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.goddy.storagetoolkit.navigation.AppNavGraph
import com.goddy.storagetoolkit.ui.theme.StorageToolkitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StorageToolkitTheme {
                AppNavGraph(app = application as StorageToolkitApp)
            }
        }
    }
}
