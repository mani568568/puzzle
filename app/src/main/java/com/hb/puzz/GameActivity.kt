package com.hb.puzz

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

@Composable
fun GameScreen(
    levelId: Int = 1,
    onBack: () -> Unit = {},
    onGameOver: (Int) -> Unit = {}
) {
    var moveCount by remember { mutableStateOf(0) }
    
    val engine by remember { mutableStateOf(PuzzleEngine(3)) }
    
    var isSolved by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5F1E8)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Level $levelId", style = MaterialTheme.typography.bodyLarge)
                Text("${engine.gridSize}x${engine.gridSize}", style = MaterialTheme.typography.bodySmall)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}) {
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
                    engine.attemptSwap(posA, posB)
                    moveCount++
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
            modifier = Modifier.fillMaxWidth(0.8f).height(48.dp)
        ) {
            Text("Restart Level")
        }
    }
}
