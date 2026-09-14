package com.hb.puzz.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Manages game settings using DataStore.
 */
class GameSettings(context: Context) {
    
    private val Context.dataStore by preferencesDataStore(name = "cozy_blocks_settings")
    private val dataStore = context.dataStore

    companion object {
        // Settings keys
        private val KEY_COMPLETED_LEVELS = stringSetPreferencesKey("completed_levels")
        private val KEY_HIGHEST_LEVEL = intPreferencesKey("highest_level")
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    // Settings flows
    val completedLevelsFlow: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[KEY_COMPLETED_LEVELS] ?: emptySet()
    }

    val highestLevelFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_HIGHEST_LEVEL] ?: 1
    }

    val soundEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SOUND_ENABLED] ?: true
    }

    val hapticsEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_HAPTICS_ENABLED] ?: false
    }

    val darkThemeFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DARK_THEME] ?: false
    }

    // Update methods
    suspend fun updateCompletedLevels(levels: Set<String>) {
        dataStore.edit { prefs ->
            prefs[KEY_COMPLETED_LEVELS] = levels
        }
    }

    suspend fun addCompletedLevel(levelId: Int) {
        val currentLevels = dataStore.data.map { it[KEY_COMPLETED_LEVELS] ?: emptySet() }.firstOrNull() ?: emptySet()
        dataStore.edit { prefs ->
            prefs[KEY_COMPLETED_LEVELS] = (currentLevels + levelId.toString()).toMutableSet()
        }
        
        // Update highest level if needed
        updateHighestLevel(levelId)
    }

    suspend fun updateHighestLevel(newLevel: Int) {
        val currentHighest = dataStore.data.map { it[KEY_HIGHEST_LEVEL] ?: 1 }.firstOrNull() ?: 1
        if (newLevel > currentHighest) {
            dataStore.edit { prefs ->
                prefs[KEY_HIGHEST_LEVEL] = newLevel
            }
        }
    }

    suspend fun updateSoundEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SOUND_ENABLED] = enabled
        }
    }

    suspend fun updateHapticsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun updateDarkTheme(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DARK_THEME] = enabled
        }
    }

    /**
     * Resets all progress.
     */
    suspend fun resetProgress() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
