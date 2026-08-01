package com.goddy.storagetoolkit.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val autoDeleteRecycleBinDays: Int = 30,
    val ignoredFolders: Set<String> = emptySet(),
    /** Only English ships today; this is stored so it's not lost once more languages land. */
    val languageTag: String = "en"
)

private val Context.settingsDataStore by preferencesDataStore(name = "storage_toolkit_settings")

class SettingsManager(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val AUTO_DELETE_DAYS = intPreferencesKey("auto_delete_recycle_bin_days")
        val IGNORED_FOLDERS = stringSetPreferencesKey("ignored_folders")
        val LANGUAGE = stringPreferencesKey("language_tag")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            theme = prefs[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            autoDeleteRecycleBinDays = prefs[Keys.AUTO_DELETE_DAYS] ?: 30,
            ignoredFolders = prefs[Keys.IGNORED_FOLDERS] ?: emptySet(),
            languageTag = prefs[Keys.LANGUAGE] ?: "en"
        )
    }

    /** Reads the ignore list once, synchronously-from-a-coroutine, for scanners about to run. */
    suspend fun currentIgnoredFolders(): Set<String> = settingsFlow.first().ignoredFolders

    suspend fun setTheme(theme: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setAutoDeleteDays(days: Int) {
        context.settingsDataStore.edit { it[Keys.AUTO_DELETE_DAYS] = days.coerceIn(1, 365) }
    }

    suspend fun addIgnoredFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.IGNORED_FOLDERS] ?: emptySet()
            prefs[Keys.IGNORED_FOLDERS] = current + trimmed
        }
    }

    suspend fun removeIgnoredFolder(name: String) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[Keys.IGNORED_FOLDERS] ?: emptySet()
            prefs[Keys.IGNORED_FOLDERS] = current - name
        }
    }
}
