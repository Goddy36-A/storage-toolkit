package com.goddy.storagetoolkit.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.goddy.storagetoolkit.StorageToolkitApp
import com.goddy.storagetoolkit.ui.apkmanager.ApkManagerScreen
import com.goddy.storagetoolkit.ui.dashboard.DashboardScreen
import com.goddy.storagetoolkit.ui.downloads.DownloadsScreen
import com.goddy.storagetoolkit.ui.duplicate.DuplicateScreen
import com.goddy.storagetoolkit.ui.emptyfolder.EmptyFolderScreen
import com.goddy.storagetoolkit.ui.largefile.LargeFileScreen
import com.goddy.storagetoolkit.ui.settings.AboutScreen
import com.goddy.storagetoolkit.ui.settings.PrivacyPolicyScreen
import com.goddy.storagetoolkit.ui.settings.SettingsScreen
import com.goddy.storagetoolkit.ui.zerobyte.ZeroByteScreen
import com.goddy.storagetoolkit.viewmodel.AppViewModelFactory
import com.goddy.storagetoolkit.viewmodel.ApkManagerViewModel
import com.goddy.storagetoolkit.viewmodel.DashboardViewModel
import com.goddy.storagetoolkit.viewmodel.DownloadsViewModel
import com.goddy.storagetoolkit.viewmodel.DuplicateViewModel
import com.goddy.storagetoolkit.viewmodel.EmptyFolderViewModel
import com.goddy.storagetoolkit.viewmodel.LargeFileViewModel
import com.goddy.storagetoolkit.viewmodel.SettingsViewModel
import com.goddy.storagetoolkit.viewmodel.ZeroByteViewModel

@Composable
fun AppNavGraph(app: StorageToolkitApp, navController: NavHostController = rememberNavController()) {
    val factory = AppViewModelFactory(app)

    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route) {
            val viewModel: DashboardViewModel = viewModel(factory = factory)
            DashboardScreen(
                viewModel = viewModel,
                onOpenDownloads = { navController.navigate(Screen.Downloads.route) },
                onOpenApkManager = { navController.navigate(Screen.ApkManager.route) },
                onOpenZeroByte = { navController.navigate(Screen.ZeroByte.route) },
                onOpenEmptyFolder = { navController.navigate(Screen.EmptyFolder.route) },
                onOpenDuplicate = { navController.navigate(Screen.Duplicate.route) },
                onOpenLargeFile = { navController.navigate(Screen.LargeFile.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Downloads.route) {
            val viewModel: DownloadsViewModel = viewModel(factory = factory)
            DownloadsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.ApkManager.route) {
            val viewModel: ApkManagerViewModel = viewModel(factory = factory)
            ApkManagerScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.ZeroByte.route) {
            val viewModel: ZeroByteViewModel = viewModel(factory = factory)
            ZeroByteScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.EmptyFolder.route) {
            val viewModel: EmptyFolderViewModel = viewModel(factory = factory)
            EmptyFolderScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Duplicate.route) {
            val viewModel: DuplicateViewModel = viewModel(factory = factory)
            DuplicateScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.LargeFile.route) {
            val viewModel: LargeFileViewModel = viewModel(factory = factory)
            LargeFileScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(factory = factory)
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenAbout = { navController.navigate(Screen.About.route) },
                onOpenPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) }
            )
        }
        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
    }
}
