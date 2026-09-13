package com.hb.puzz.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hb.puzz.data.GameDatastore
import com.hb.puzz.domain.GameEngine
import com.hb.puzz.domain.model.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException

class GameViewModel(application: Application) : AndroidViewModel(application) {
    val settings = GameDatastore(application)
    private val engine = GameEngine()
    private val pendingSaves = Channel<GameState>(Channel.CONFLATED)
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            for (state in pendingSaves) {
                try { settings.saveState(state) }
                catch (_: IOException) { _uiState.value = _uiState.value.copy(saveError = true) }
            }
        }
        viewModelScope.launch {
            try {
                val saved = settings.loadState()
                if (saved != null) engine.restoreState(saved)
                engine.updateBestScore(settings.bestScoreFlow.first())
                publish(hasSavedGame = saved != null)
            } catch (_: IOException) {
                publish(hasSavedGame = false)
                _uiState.value = _uiState.value.copy(saveError = true)
            }
        }
    }

    private fun publish(hasSavedGame: Boolean = true) {
        val s = engine.state
        _uiState.value = GameUiState(s.board, s.pieces, s.score, s.bestScore,
            s.isGameOver, true, hasSavedGame, _uiState.value.saveError)
    }

    fun tryPlacePiece(piece: Piece, x: Int, y: Int): Boolean {
        if (!_uiState.value.isLoaded) return false
        val placed = engine.tryPlacePiece(piece, x, y)
        if (placed) { publish(); pendingSaves.trySend(engine.state) }
        return placed
    }

    fun resetGame() {
        if (!_uiState.value.isLoaded) return
        engine.resetGame()
        publish()
        pendingSaves.trySend(engine.state)
    }

    fun sound(enabled: Boolean) { viewModelScope.launch { settings.updateSoundEnabled(enabled) } }
    fun haptics(enabled: Boolean) { viewModelScope.launch { settings.updateHapticsEnabled(enabled) } }
    fun darkTheme(enabled: Boolean) { viewModelScope.launch { settings.updateDarkTheme(enabled) } }
}

data class GameUiState(
    val board: GameBoard = GameBoard.createEmptyBoard(),
    val pieces: List<Piece> = emptyList(),
    val score: Int = 0,
    val bestScore: Int = 0,
    val isGameOver: Boolean = false,
    val isLoaded: Boolean = false,
    val hasSavedGame: Boolean = false,
    val saveError: Boolean = false
)
