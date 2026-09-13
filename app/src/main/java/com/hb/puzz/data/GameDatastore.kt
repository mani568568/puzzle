package com.hb.puzz.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.edit
import com.hb.puzz.domain.model.GameState
import kotlinx.coroutines.flow.first
import com.hb.puzz.domain.model.Cell
import com.hb.puzz.domain.model.GameBoard
import com.hb.puzz.domain.model.Piece
import com.hb.puzz.domain.model.PieceColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Manages game data persistence using DataStore.
 */
private val Context.gameDataStore by preferencesDataStore(name = "game_settings")

class GameDatastore(context: Context) {
    
    private val dataStore: DataStore<Preferences> = context.applicationContext.gameDataStore

    companion object {
        // Settings keys
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        
        // Game state keys
        private val KEY_SCORE = intPreferencesKey("current_score")
        private val KEY_BEST_SCORE = intPreferencesKey("best_score")
        private val KEY_BOARD_STATE = stringPreferencesKey("board_state")
        private val KEY_PIECES_STATE = stringPreferencesKey("pieces_state")
        
        fun serializeBoard(board: GameBoard): String = GameStateCodec.serializeBoard(board)
        fun parseBoard(serialized: String): GameBoard = GameStateCodec.parseBoard(serialized)
        fun serializePieces(pieces: List<Piece>): String = GameStateCodec.serializePieces(pieces)
        fun parsePieces(serialized: String): List<Piece> = GameStateCodec.parsePieces(serialized)

    }

    // Settings flows
    val soundEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SOUND_ENABLED] ?: true
    }

    val hapticsEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_HAPTICS_ENABLED] ?: true
    }

    val darkThemeFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DARK_THEME] ?: false
    }

    // Game state flows
    val currentScoreFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_SCORE] ?: 0
    }

    val bestScoreFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_BEST_SCORE] ?: 0
    }

    val savedGameBoardFlow: Flow<GameBoard?> = dataStore.data.map { prefs ->
        prefs[KEY_BOARD_STATE]?.let { serialize ->
            if (serialize.isEmpty()) null else parseBoard(serialize)
        }
    }

    val savedPiecesFlow: Flow<List<Piece>?> = dataStore.data.map { prefs ->
        prefs[KEY_PIECES_STATE]?.let { serialize ->
            if (serialize.isEmpty()) null else parsePieces(serialize)
        }
    }

    suspend fun loadState(): GameState? {
        val prefs = dataStore.data.first()
        val board = prefs[KEY_BOARD_STATE] ?: return null
        val pieces = parsePieces(prefs[KEY_PIECES_STATE] ?: "")
        if (pieces.isEmpty()) return null
        return GameState(parseBoard(board), pieces, prefs[KEY_SCORE] ?: 0, prefs[KEY_BEST_SCORE] ?: 0)
    }

    suspend fun saveState(state: GameState) {
        dataStore.edit { prefs ->
            prefs[KEY_BOARD_STATE] = serializeBoard(state.board)
            prefs[KEY_PIECES_STATE] = serializePieces(state.pieces)
            prefs[KEY_SCORE] = state.score
            prefs[KEY_BEST_SCORE] = maxOf(prefs[KEY_BEST_SCORE] ?: 0, state.bestScore)
        }
    }

    // Update methods
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

    suspend fun updateScore(score: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_SCORE] = score
        }
    }

    suspend fun updateBestScore(bestScore: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_BEST_SCORE] = maxOf(prefs[KEY_BEST_SCORE] ?: 0, bestScore)
        }
    }

    suspend fun saveGameState(board: GameBoard, pieces: List<Piece>) {
        dataStore.edit { prefs ->
            prefs[KEY_BOARD_STATE] = serializeBoard(board)
            prefs[KEY_PIECES_STATE] = serializePieces(pieces)
        }
    }

    suspend fun clearSavedGame() {
        dataStore.edit { prefs ->
            prefs[KEY_BOARD_STATE] = ""
            prefs[KEY_PIECES_STATE] = ""
            prefs[KEY_SCORE] = 0
        }
    }
}
