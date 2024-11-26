package com.example.myapplication.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore instance
private val Context.dataStore by preferencesDataStore(name = "theme_preferences")

object ThemePreferences {
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")

    fun isDarkModeEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }
    }

    suspend fun setDarkMode(context: Context, isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isEnabled
        }
    }
}