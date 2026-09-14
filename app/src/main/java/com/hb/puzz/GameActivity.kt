package com.hb.puzz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hb.puzz.domain.PuzzleEngine
import com.hb.puzz.domain.PuzzleLevel
import com.hb.puzz.ui.PuzzleBoard
import com.hb.puzz.ui.theme.CozyBlocksTheme

/** Activity that hosts the picture-puzzle game. */
class GameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requestedLevel = intent.getIntExtra(EXTRA_LEVEL_ID, 1)

        setContent {
            CozyBlocksTheme {
                PicturePuzzleGameScreen(
                    initialLevelId = requestedLevel,
                    onBack = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_LEVEL_ID = "level_id"
    }
}

@Composable
fun PicturePuzzleGameScreen(
    initialLevelId: Int = 1,
    onBack: () -> Unit = {}
) {
    var levelId by remember { mutableIntStateOf(initialLevelId.coerceIn(1, PuzzleLevel.maxLevelId)) }
    val level = PuzzleLevel.requireLevel(levelId)
    val engine = remember(level.id) { PuzzleEngine(level.gridSize, level.seed) }
    var moveCount by remember(level.id) { mutableIntStateOf(0) }
    var boardVersion by remember(level.id) { mutableIntStateOf(0) }
    var isSolved by remember(level.id) { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    fun restart() {
        engine.shuffle()
        moveCount = 0
        isSolved = false
        boardVersion++
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Level ${level.id}: ${level.title}", style = MaterialTheme.typography.titleMedium)
                Text("${level.gridSize} × ${level.gridSize} • Moves: $moveCount", style = MaterialTheme.typography.bodySmall)
            }

            IconButton(onClick = { isPaused = true }, enabled = !isSolved) {
                Icon(Icons.Default.Pause, contentDescription = "Pause")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!isSolved) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                key(boardVersion) {
                    PuzzleBoard(
                        engine = engine,
                        levelId = level.id,
                        onTileSwapped = { posA, posB ->
                            if (engine.attemptSwap(posA, posB)) {
                                moveCount++
                                boardVersion++
                                isSolved = engine.isSolved()
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Puzzle Solved!", style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Completed in $moveCount moves")
                Spacer(modifier = Modifier.height(24.dp))

                if (level.id < PuzzleLevel.maxLevelId) {
                    Button(onClick = { levelId++ }) {
                        Text("Next Level")
                    }
                } else {
                    Button(onClick = onBack) {
                        Text("Finish")
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { restart() },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(vertical = 16.dp)
        ) {
            Text("Restart Level")
        }
    }

    if (isPaused) {
        AlertDialog(
            onDismissRequest = { isPaused = false },
            title = { Text("Paused") },
            text = { Text("Resume, restart this puzzle, or return home.") },
            confirmButton = {
                Button(onClick = { isPaused = false }) { Text("Resume") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        restart()
                        isPaused = false
                    }) { Text("Restart") }
                    TextButton(onClick = onBack) { Text("Home") }
                }
            }
        )
    }
}
