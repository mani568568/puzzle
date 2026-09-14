package com.hb.puzz.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hb.puzz.data.images.ImageSourceMode
import com.hb.puzz.data.images.PexelsPhotoMeta
import com.hb.puzz.domain.PuzzleLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.picturePuzzleDataStore by preferencesDataStore(name = "cozy_blocks_settings")

/** Persisted in-progress picture-puzzle session. */
enum class PuzzleClockMode(val storedValue: String) {
    COUNTDOWN("countdown"),
    STOPWATCH("stopwatch");

    companion object {
        fun fromStored(value: String?): PuzzleClockMode =
            values().firstOrNull { it.storedValue == value } ?: COUNTDOWN
    }
}

data class PuzzleSession(
    val levelId: Int,
    val gridSize: Int,
    val positions: IntArray,
    val moveCount: Int,
    val remainingSeconds: Int? = null,
    val clockMode: PuzzleClockMode = PuzzleClockMode.COUNTDOWN,
    val stopwatchElapsedSeconds: Int = 0,
    val timerStarted: Boolean = false,
    val totalChallengeSeconds: Int? = null
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
        private val KEY_IMAGE_SOURCE = stringPreferencesKey("image_source")

        private val KEY_SAVED_LEVEL = intPreferencesKey("saved_level")
        private val KEY_SAVED_GRID_SIZE = intPreferencesKey("saved_grid_size")
        private val KEY_SAVED_POSITIONS = stringPreferencesKey("saved_positions")
        private val KEY_SAVED_MOVES = intPreferencesKey("saved_moves")
        private val KEY_SAVED_REMAINING_SECONDS = intPreferencesKey("saved_remaining_seconds")
        private val KEY_SAVED_CLOCK_MODE = stringPreferencesKey("saved_clock_mode")
        private val KEY_SAVED_STOPWATCH_SECONDS = intPreferencesKey("saved_stopwatch_seconds")
        private val KEY_SAVED_TIMER_STARTED = booleanPreferencesKey("saved_timer_started")
        private val KEY_SAVED_TOTAL_CHALLENGE_SECONDS = intPreferencesKey("saved_total_challenge_seconds")

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
    val imageSourceFlow: Flow<ImageSourceMode> = dataStore.data.map { prefs ->
        ImageSourceMode.fromStored(prefs[KEY_IMAGE_SOURCE])
    }

    val savedSessionFlow: Flow<PuzzleSession?> = dataStore.data.map { prefs ->
        decodeSession(
            levelId = prefs[KEY_SAVED_LEVEL],
            gridSize = prefs[KEY_SAVED_GRID_SIZE],
            positionsText = prefs[KEY_SAVED_POSITIONS],
            moveCount = prefs[KEY_SAVED_MOVES] ?: 0,
            remainingSeconds = prefs[KEY_SAVED_REMAINING_SECONDS],
            clockMode = prefs[KEY_SAVED_CLOCK_MODE],
            stopwatchElapsedSeconds = prefs[KEY_SAVED_STOPWATCH_SECONDS] ?: 0,
            timerStarted = prefs[KEY_SAVED_TIMER_STARTED] ?: false,
            totalChallengeSeconds = prefs[KEY_SAVED_TOTAL_CHALLENGE_SECONDS]
        )
    }

    suspend fun loadSession(): PuzzleSession? {
        val prefs = dataStore.data.first()
        return decodeSession(
            levelId = prefs[KEY_SAVED_LEVEL],
            gridSize = prefs[KEY_SAVED_GRID_SIZE],
            positionsText = prefs[KEY_SAVED_POSITIONS],
            moveCount = prefs[KEY_SAVED_MOVES] ?: 0,
            remainingSeconds = prefs[KEY_SAVED_REMAINING_SECONDS],
            clockMode = prefs[KEY_SAVED_CLOCK_MODE],
            stopwatchElapsedSeconds = prefs[KEY_SAVED_STOPWATCH_SECONDS] ?: 0,
            timerStarted = prefs[KEY_SAVED_TIMER_STARTED] ?: false,
            totalChallengeSeconds = prefs[KEY_SAVED_TOTAL_CHALLENGE_SECONDS]
        )
    }

    private fun decodeSession(
        levelId: Int?,
        gridSize: Int?,
        positionsText: String?,
        moveCount: Int,
        remainingSeconds: Int?,
        clockMode: String?,
        stopwatchElapsedSeconds: Int,
        timerStarted: Boolean,
        totalChallengeSeconds: Int?
    ): PuzzleSession? {
        val id = levelId ?: return null
        val level = PuzzleLevel.getLevel(id) ?: return null
        val positions = positionsText?.let(::decodePositions) ?: return null

        // Backward compatibility: older saves did not persist grid size. Infer it from the tile count.
        val inferredGridSize = kotlin.math.sqrt(positions.size.toDouble()).toInt()
            .takeIf { it * it == positions.size }
        val resolvedGridSize = gridSize ?: inferredGridSize ?: return null

        if (!level.acceptsGridSize(resolvedGridSize)) return null
        if (positions.size != resolvedGridSize * resolvedGridSize) return null
        if (positions.toSet().size != positions.size) return null
        if (positions.any { it !in positions.indices }) return null
        return PuzzleSession(
            id,
            resolvedGridSize,
            positions,
            moveCount.coerceAtLeast(0),
            remainingSeconds?.coerceAtLeast(0),
            PuzzleClockMode.fromStored(clockMode),
            stopwatchElapsedSeconds.coerceAtLeast(0),
            timerStarted,
            totalChallengeSeconds?.coerceAtLeast(0)
        )
    }

    suspend fun saveSession(
        levelId: Int,
        gridSize: Int,
        positions: IntArray,
        moveCount: Int,
        remainingSeconds: Int? = null,
        clockMode: PuzzleClockMode = PuzzleClockMode.COUNTDOWN,
        stopwatchElapsedSeconds: Int = 0,
        timerStarted: Boolean = false,
        totalChallengeSeconds: Int? = null
    ) {
        val level = PuzzleLevel.getLevel(levelId) ?: return
        if (!level.acceptsGridSize(gridSize)) return
        if (positions.size != gridSize * gridSize) return
        if (positions.toSet().size != positions.size || positions.any { it !in positions.indices }) return

        dataStore.edit { prefs ->
            prefs[KEY_SAVED_LEVEL] = levelId
            prefs[KEY_SAVED_GRID_SIZE] = gridSize
            prefs[KEY_SAVED_POSITIONS] = encodePositions(positions)
            prefs[KEY_SAVED_MOVES] = moveCount.coerceAtLeast(0)
            prefs[KEY_SAVED_CLOCK_MODE] = clockMode.storedValue
            prefs[KEY_SAVED_STOPWATCH_SECONDS] = stopwatchElapsedSeconds.coerceAtLeast(0)
            prefs[KEY_SAVED_TIMER_STARTED] = timerStarted
            if (totalChallengeSeconds != null) {
                prefs[KEY_SAVED_TOTAL_CHALLENGE_SECONDS] = totalChallengeSeconds.coerceAtLeast(0)
            } else {
                prefs.remove(KEY_SAVED_TOTAL_CHALLENGE_SECONDS)
            }
            if (remainingSeconds != null) {
                prefs[KEY_SAVED_REMAINING_SECONDS] = remainingSeconds.coerceAtLeast(0)
            } else {
                prefs.remove(KEY_SAVED_REMAINING_SECONDS)
            }
        }
    }

    suspend fun clearSession() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_SAVED_LEVEL)
            prefs.remove(KEY_SAVED_GRID_SIZE)
            prefs.remove(KEY_SAVED_POSITIONS)
            prefs.remove(KEY_SAVED_MOVES)
            prefs.remove(KEY_SAVED_REMAINING_SECONDS)
            prefs.remove(KEY_SAVED_CLOCK_MODE)
            prefs.remove(KEY_SAVED_STOPWATCH_SECONDS)
            prefs.remove(KEY_SAVED_TIMER_STARTED)
            prefs.remove(KEY_SAVED_TOTAL_CHALLENGE_SECONDS)
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

    suspend fun updateImageSource(mode: ImageSourceMode) {
        dataStore.edit { it[KEY_IMAGE_SOURCE] = mode.storedValue }
    }

    suspend fun loadPexelsPhoto(levelId: Int): PexelsPhotoMeta? {
        val prefs = dataStore.data.first()
        val id = prefs[stringPreferencesKey("pexels_${levelId}_id")]?.toLongOrNull() ?: return null
        val imageUrl = prefs[stringPreferencesKey("pexels_${levelId}_image_url")] ?: return null
        val photoUrl = prefs[stringPreferencesKey("pexels_${levelId}_photo_url")] ?: return null
        val photographer = prefs[stringPreferencesKey("pexels_${levelId}_photographer")] ?: "Pexels photographer"
        val photographerUrl = prefs[stringPreferencesKey("pexels_${levelId}_photographer_url")] ?: ""
        return PexelsPhotoMeta(id, imageUrl, photoUrl, photographer, photographerUrl)
    }

    suspend fun savePexelsPhoto(levelId: Int, photo: PexelsPhotoMeta) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey("pexels_${levelId}_id")] = photo.id.toString()
            prefs[stringPreferencesKey("pexels_${levelId}_image_url")] = photo.imageUrl
            prefs[stringPreferencesKey("pexels_${levelId}_photo_url")] = photo.photoUrl
            prefs[stringPreferencesKey("pexels_${levelId}_photographer")] = photo.photographer
            prefs[stringPreferencesKey("pexels_${levelId}_photographer_url")] = photo.photographerUrl
        }
    }

    suspend fun clearPexelsPhoto(levelId: Int) {
        dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey("pexels_${levelId}_id"))
            prefs.remove(stringPreferencesKey("pexels_${levelId}_image_url"))
            prefs.remove(stringPreferencesKey("pexels_${levelId}_photo_url"))
            prefs.remove(stringPreferencesKey("pexels_${levelId}_photographer"))
            prefs.remove(stringPreferencesKey("pexels_${levelId}_photographer_url"))
        }
    }

    /** Clears game progress and saved puzzle only; user preference toggles are preserved. */
    suspend fun resetProgress() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_COMPLETED_LEVELS)
            prefs.remove(KEY_HIGHEST_LEVEL)
            prefs.remove(KEY_SAVED_LEVEL)
            prefs.remove(KEY_SAVED_GRID_SIZE)
            prefs.remove(KEY_SAVED_POSITIONS)
            prefs.remove(KEY_SAVED_MOVES)
            prefs.remove(KEY_SAVED_REMAINING_SECONDS)
            prefs.remove(KEY_SAVED_CLOCK_MODE)
            prefs.remove(KEY_SAVED_STOPWATCH_SECONDS)
            prefs.remove(KEY_SAVED_TIMER_STARTED)
            prefs.remove(KEY_SAVED_TOTAL_CHALLENGE_SECONDS)
        }
    }
}
