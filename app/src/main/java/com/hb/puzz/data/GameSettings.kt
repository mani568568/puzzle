package com.hb.puzz.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.picturePuzzleDataStore by preferencesDataStore(name = "cozy_blocks_settings")

/** Persistence for picture-puzzle progress and user preferences. */
class GameSettings(context: Context) {
    private val dataStore = context.applicationContext.picturePuzzleDataStore

    companion object {
        private val KEY_COMPLETED_LEVELS = stringSetPreferencesKey("completed_levels")
        private val KEY_HIGHEST_LEVEL = intPreferencesKey("highest_level")
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val completedLevelsFlow: Flow<Set<String>> = dataStore.data.map { it[KEY_COMPLETED_LEVELS] ?: emptySet() }
    val highestLevelFlow: Flow<Int> = dataStore.data.map { it[KEY_HIGHEST_LEVEL] ?: 1 }
    val soundEnabledFlow: Flow<Boolean> = dataStore.data.map { it[KEY_SOUND_ENABLED] ?: true }
    val hapticsEnabledFlow: Flow<Boolean> = dataStore.data.map { it[KEY_HAPTICS_ENABLED] ?: false }
    val darkThemeFlow: Flow<Boolean> = dataStore.data.map { it[KEY_DARK_THEME] ?: false }

    suspend fun updateCompletedLevels(levels: Set<String>) {
        dataStore.edit { it[KEY_COMPLETED_LEVELS] = levels }
    }

    suspend fun addCompletedLevel(levelId: Int) {
        val current = dataStore.data.first()[KEY_COMPLETED_LEVELS] ?: emptySet()
        dataStore.edit { it[KEY_COMPLETED_LEVELS] = current + levelId.toString() }
        updateHighestLevel(levelId + 1)
    }

    suspend fun updateHighestLevel(newLevel: Int) {
        val current = dataStore.data.first()[KEY_HIGHEST_LEVEL] ?: 1
        if (newLevel > current) {
            dataStore.edit { it[KEY_HIGHEST_LEVEL] = newLevel.coerceAtMost(20) }
        }
    }

    suspend fun updateSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SOUND_ENABLED] = enabled }
    }

    suspend fun updateHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_HAPTICS_ENABLED] = enabled }
    }

    suspend fun updateDarkTheme(enabled: Boolean) {
        dataStore.edit { it[KEY_DARK_THEME] = enabled }
    }

    suspend fun resetProgress() {
        dataStore.edit { it.clear() }
    }
}
