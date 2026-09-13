package com.hb.puzz

import android.os.Bundle
import android.view.SoundEffectConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hb.puzz.ui.*
import com.hb.puzz.ui.theme.MosaicBlocksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MosaicBlocksRoot() }
    }
}

@Composable
fun MosaicBlocksRoot(model: GameViewModel = viewModel()) {
    val state by model.uiState.collectAsState()
    val sound by model.settings.soundEnabledFlow.collectAsState(initial = true)
    val haptics by model.settings.hapticsEnabledFlow.collectAsState(initial = true)
    val dark by model.settings.darkThemeFlow.collectAsState(initial = false)
    var screen by rememberSaveable { mutableStateOf("home") }
    val view = LocalView.current
    val feedback = LocalHapticFeedback.current
    BackHandler(enabled = screen != "home") {
        screen = when (screen) { "game" -> "pause"; "pause" -> "game"; else -> "home" }
    }
    MosaicBlocksTheme(darkTheme = dark) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                if (!state.isLoaded) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else when (screen) {
                    "home" -> HomeScreen(
                        hasSavedGame = state.hasSavedGame,
                        onContinue = { screen = "game" },
                        onStartNewGame = { model.resetGame(); screen = "game" },
                        onHowToPlay = { screen = "help" },
                        onSettings = { screen = "settings" }
                    )
                    "game" -> if (state.isGameOver) {
                        GameOverScreen(state.score, state.bestScore,
                            onPlayAgain = { model.resetGame() }, onHome = { screen = "home" })
                    } else {
                        GameScreen(state = state, onPiecePlaced = { piece, x, y ->
                            val placed = model.tryPlacePiece(piece, x, y)
                            if (placed) {
                                if (sound) view.playSoundEffect(SoundEffectConstants.CLICK)
                                if (haptics) feedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            placed
                        }, onPaused = { screen = "pause" })
                    }
                    "pause" -> PauseScreen(onResume = { screen = "game" },
                        onRestart = { model.resetGame(); screen = "game" }, onHome = { screen = "home" })
                    "help" -> Column {
                        TextButton(onClick = { screen = "home" }) { Text("Back") }
                        HowToPlayScreen(Modifier.weight(1f))
                    }
                    "settings" -> Column {
                        TextButton(onClick = { screen = "home" }) { Text("Back") }
                        SettingsScreen(sound, haptics, dark, model::sound, model::haptics,
                            model::darkTheme, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
