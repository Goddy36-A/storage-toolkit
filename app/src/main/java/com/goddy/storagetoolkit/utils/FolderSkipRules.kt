package com.goddy.storagetoolkit.utils

/**
 * Centralizes which folder names every scanner skips: hidden folders (leading "."),
 * "Android" (protected by scoped storage regardless), and whatever the user has added
 * to their ignore list in Settings. Matching is case-insensitive by name only (not
 * full path) -- simple and predictable, matching how the Settings screen presents it.
 */
object FolderSkipRules {
    val builtIn = setOf("Android")

    fun shouldSkip(folderName: String, userIgnoredFolders: Set<String>): Boolean {
        if (folderName.startsWith(".")) return true
        if (builtIn.any { it.equals(folderName, ignoreCase = true) }) return true
        return userIgnoredFolders.any { it.equals(folderName, ignoreCase = true) }
    }
}
