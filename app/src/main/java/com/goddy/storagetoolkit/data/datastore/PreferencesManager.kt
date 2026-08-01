package com.goddy.storagetoolkit.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "storage_toolkit_prefs")

class PreferencesManager(private val context: Context) {

    private val safTreeUriKey = stringPreferencesKey("saf_tree_uri")

    val safTreeUriFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[safTreeUriKey]
    }

    suspend fun setSafTreeUri(uri: String) {
        context.dataStore.edit { prefs ->
            prefs[safTreeUriKey] = uri
        }
    }

    suspend fun clearSafTreeUri() {
        context.dataStore.edit { prefs ->
            prefs.remove(safTreeUriKey)
        }
    }
}
