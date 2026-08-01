package com.goddy.storagetoolkit.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Downloads : Screen("downloads")
    data object ApkManager : Screen("apk_manager")
}
