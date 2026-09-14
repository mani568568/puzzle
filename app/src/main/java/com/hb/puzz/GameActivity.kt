package com.hb.puzz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hb.puzz.domain.PuzzleEngine
import com.hb.puzz.ui.PuzzleBoard

/**
 * Activity that hosts the picture-puzzle game screen.
 *
 * This class is declared in AndroidManifest.xml as `.GameActivity`, so it must
 * extend Activity/ComponentActivity rather than being only a composable file.
 */
class GameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val levelId = intent.getIntExtra(EXTRA_LEVEL_ID, 1)

        setContent {
            MaterialTheme {
                PicturePuzzleGameScreen(
                    levelId = levelId,
                    onBack = { finish() },
                    onGameOver = { /* Completion navigation will be wired later. */ }
                )
            }
        }
    }

    companion object {
        const val EXTRA_LEVEL_ID = "level_id"
    }
}

/**
 * UI for the picture-puzzle game hosted by [GameActivity].
 */
@Composable
fun PicturePuzzleGameScreen(
    levelId: Int = 1,
    onBack: () -> Unit = {},
    onGameOver: (Int) -> Unit = {}
) {
    var moveCount by remember { mutableIntStateOf(0) }
    val engine = remember { PuzzleEngine(3) }
    var isSolved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F1E8)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Level $levelId", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${engine.gridSize}x${engine.gridSize}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { /* Pause behavior will be fixed separately. */ }) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Moves: $moveCount", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .weight(2f)
                .fillMaxWidth()
                .background(Color(0xFFF5F1E8)),
            contentAlignment = Alignment.Center
        ) {
            if (!isSolved) {
                PuzzleBoard(engine) { posA, posB ->
                    if (engine.attemptSwap(posA, posB)) {
                        moveCount++
                    }

                    if (engine.isSolved()) {
                        isSolved = true
                        onGameOver(moveCount)
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Puzzle Solved!", style = MaterialTheme.typography.headlineLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Moves: $moveCount", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                engine.shuffle()
                moveCount = 0
                isSolved = false
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
        ) {
            Text("Restart Level")
        }
    }
}
