package com.hb.puzz.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.hb.puzz.data.GameSettings
import com.hb.puzz.domain.PuzzleEngine
import com.hb.puzz.domain.PuzzleLevel
import kotlinx.coroutines.launch

@Composable
fun PicturePuzzleGameScreen(
    levelId: Int,
    settings: GameSettings,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onNextLevel: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val level = PuzzleLevel.requireLevel(levelId)
    val engine = remember(level.id) { PuzzleEngine(level.gridSize, level.seed) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 35) }

    var moveCount by remember(level.id) { mutableIntStateOf(0) }
    var boardVersion by remember(level.id) { mutableIntStateOf(0) }
    var isSolved by remember(level.id) { mutableStateOf(false) }
    var isPaused by remember(level.id) { mutableStateOf(false) }
    var initialized by remember(level.id) { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    LaunchedEffect(level.id) {
        val saved = settings.loadSession()
        if (saved?.levelId == level.id && engine.restorePositions(saved.positions)) {
            moveCount = saved.moveCount
            isSolved = engine.isSolved()
        } else {
            settings.saveSession(level.id, engine.getCurrentPositions(), 0)
        }
        boardVersion++
        initialized = true
    }

    fun restart() {
        engine.shuffle()
        moveCount = 0
        isSolved = false
        boardVersion++
        scope.launch { settings.saveSession(level.id, engine.getCurrentPositions(), 0) }
    }

    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Level ${level.id}: ${level.title}", style = MaterialTheme.typography.titleMedium)
                Text("${level.gridSize} × ${level.gridSize} · Moves: $moveCount", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { isPaused = true }, enabled = initialized && !isSolved) {
                Icon(Icons.Default.Pause, contentDescription = "Pause")
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            !initialized -> Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            !isSolved -> Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                key(boardVersion) {
                    PuzzleBoard(
                        engine = engine,
                        levelId = level.id,
                        onTileSwapped = { posA, posB ->
                            if (engine.attemptSwap(posA, posB)) {
                                moveCount++
                                boardVersion++
                                if (soundEnabled) toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 45)
                                if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                                val solvedNow = engine.isSolved()
                                isSolved = solvedNow
                                scope.launch {
                                    if (solvedNow) {
                                        settings.markLevelCompleted(level.id)
                                        settings.clearSession()
                                    } else {
                                        settings.saveSession(level.id, engine.getCurrentPositions(), moveCount)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            else -> Column(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Puzzle Solved!", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(12.dp))
                Text("Completed in $moveCount moves")
                Spacer(Modifier.height(24.dp))
                if (level.id < PuzzleLevel.maxLevelId) {
                    Button(onClick = { onNextLevel(level.id + 1) }) { Text("Next Level") }
                } else {
                    Button(onClick = onHome) { Text("Finish") }
                }
            }
        }

        OutlinedButton(
            onClick = { restart() },
            enabled = initialized,
            modifier = Modifier.fillMaxWidth(0.8f).padding(vertical = 16.dp)
        ) { Text("Restart Level") }
    }

    if (isPaused) {
        AlertDialog(
            onDismissRequest = { isPaused = false },
            title = { Text("Paused") },
            text = { Text("Resume, restart this puzzle, or return home.") },
            confirmButton = { Button(onClick = { isPaused = false }) { Text("Resume") } },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        restart()
                        isPaused = false
                    }) { Text("Restart") }
                    TextButton(onClick = onHome) { Text("Home") }
                }
            }
        )
    }
}
