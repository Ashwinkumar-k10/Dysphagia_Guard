package com.dysphagiaguard.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "device_settings")

class DeviceRepository(private val context: Context) {
    companion object {
        val SILENT_MODE = booleanPreferencesKey("silent_mode")
        val AUTO_PDF = booleanPreferencesKey("auto_pdf")
        val DEMO_MODE = booleanPreferencesKey("demo_mode")
    }

    val isSilentMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SILENT_MODE] ?: false
    }

    val isAutoPdfEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_PDF] ?: true
    }

    suspend fun setSilentMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SILENT_MODE] = enabled
        }
    }

    suspend fun setAutoPdf(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_PDF] = enabled
        }
    }

    val isDemoMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEMO_MODE] ?: false
    }

    suspend fun setDemoMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEMO_MODE] = enabled
        }
    }
}
