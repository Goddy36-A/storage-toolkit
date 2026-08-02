package com.goddy.storagetoolkit.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Downloads : Screen("downloads")
    data object ApkManager : Screen("apk_manager")
    data object ZeroByte : Screen("zero_byte")
    data object EmptyFolder : Screen("empty_folder")
    data object Duplicate : Screen("duplicate")
    data object LargeFile : Screen("large_file")
    data object Settings : Screen("settings")
    data object About : Screen("about")
    data object PrivacyPolicy : Screen("privacy_policy")
    data object RecycleBin : Screen("recycle_bin")
    data object StorageAnalyzer : Screen("storage_analyzer")
    data object Search : Screen("search")
    data object ScanHistory : Screen("scan_history")
}
