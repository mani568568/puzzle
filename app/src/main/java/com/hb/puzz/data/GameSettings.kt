package com.hb.puzz.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hb.puzz.domain.PuzzleLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.picturePuzzleDataStore by preferencesDataStore(name = "cozy_blocks_settings")

/** Persisted in-progress picture-puzzle session. */
data class PuzzleSession(
    val levelId: Int,
    val positions: IntArray,
    val moveCount: Int
)

/** Single source of truth for picture-puzzle progress and user preferences. */
class GameSettings(context: Context) {
    private val dataStore = context.applicationContext.picturePuzzleDataStore

    companion object {
        private val KEY_COMPLETED_LEVELS = stringSetPreferencesKey("completed_levels")
        private val KEY_HIGHEST_LEVEL = intPreferencesKey("highest_level")
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")

        private val KEY_SAVED_LEVEL = intPreferencesKey("saved_level")
        private val KEY_SAVED_POSITIONS = stringPreferencesKey("saved_positions")
        private val KEY_SAVED_MOVES = intPreferencesKey("saved_moves")

        private fun encodePositions(positions: IntArray): String = positions.joinToString(",")

        private fun decodePositions(value: String): IntArray? {
            if (value.isBlank()) return null
            return runCatching {
                value.split(',').map { it.toInt() }.toIntArray()
            }.getOrNull()
        }
    }

    val completedLevelsFlow: Flow<Set<Int>> = dataStore.data.map { prefs ->
        (prefs[KEY_COMPLETED_LEVELS] ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet()
    }

    val highestLevelFlow: Flow<Int> = dataStore.data.map { prefs ->
        (prefs[KEY_HIGHEST_LEVEL] ?: 1).coerceIn(1, PuzzleLevel.maxLevelId)
    }

    val soundEnabledFlow: Flow<Boolean> = dataStore.data.map { it[KEY_SOUND_ENABLED] ?: true }
    val hapticsEnabledFlow: Flow<Boolean> = dataStore.data.map { it[KEY_HAPTICS_ENABLED] ?: true }
    val darkThemeFlow: Flow<Boolean> = dataStore.data.map { it[KEY_DARK_THEME] ?: false }

    val savedSessionFlow: Flow<PuzzleSession?> = dataStore.data.map { prefs ->
        decodeSession(
            levelId = prefs[KEY_SAVED_LEVEL],
            positionsText = prefs[KEY_SAVED_POSITIONS],
            moveCount = prefs[KEY_SAVED_MOVES] ?: 0
        )
    }

    suspend fun loadSession(): PuzzleSession? {
        val prefs = dataStore.data.first()
        return decodeSession(
            levelId = prefs[KEY_SAVED_LEVEL],
            positionsText = prefs[KEY_SAVED_POSITIONS],
            moveCount = prefs[KEY_SAVED_MOVES] ?: 0
        )
    }

    private fun decodeSession(
        levelId: Int?,
        positionsText: String?,
        moveCount: Int
    ): PuzzleSession? {
        val id = levelId ?: return null
        val level = PuzzleLevel.getLevel(id) ?: return null
        val positions = positionsText?.let(::decodePositions) ?: return null
        if (positions.size != level.gridSize * level.gridSize) return null
        if (positions.toSet().size != positions.size) return null
        if (positions.any { it !in positions.indices }) return null
        return PuzzleSession(id, positions, moveCount.coerceAtLeast(0))
    }

    suspend fun saveSession(levelId: Int, positions: IntArray, moveCount: Int) {
        val level = PuzzleLevel.getLevel(levelId) ?: return
        if (positions.size != level.gridSize * level.gridSize) return
        if (positions.toSet().size != positions.size || positions.any { it !in positions.indices }) return

        dataStore.edit { prefs ->
            prefs[KEY_SAVED_LEVEL] = levelId
            prefs[KEY_SAVED_POSITIONS] = encodePositions(positions)
            prefs[KEY_SAVED_MOVES] = moveCount.coerceAtLeast(0)
        }
    }

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_SAVED_LEVEL)
            prefs.remove(KEY_SAVED_POSITIONS)
            prefs.remove(KEY_SAVED_MOVES)
        }
    }

    suspend fun markLevelCompleted(levelId: Int) {
        if (PuzzleLevel.getLevel(levelId) == null) return
        dataStore.edit { prefs ->
            val completed = prefs[KEY_COMPLETED_LEVELS] ?: emptySet()
            prefs[KEY_COMPLETED_LEVELS] = completed + levelId.toString()

            val unlocked = (levelId + 1).coerceAtMost(PuzzleLevel.maxLevelId)
            val currentHighest = prefs[KEY_HIGHEST_LEVEL] ?: 1
            if (unlocked > currentHighest) prefs[KEY_HIGHEST_LEVEL] = unlocked
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

    /** Clears game progress and saved puzzle only; user preference toggles are preserved. */
    suspend fun resetProgress() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_COMPLETED_LEVELS)
            prefs.remove(KEY_HIGHEST_LEVEL)
            prefs.remove(KEY_SAVED_LEVEL)
            prefs.remove(KEY_SAVED_POSITIONS)
            prefs.remove(KEY_SAVED_MOVES)
        }
    }
}
