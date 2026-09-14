package com.hb.puzz.ui.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual pulse effect when tiles form a connection.
 */
@Composable
fun MergingPulseEffect(
    groups: List<List<Int>>,
    gridSize: Int,
    cellSize: Dp,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val animationState = remember { mutableStateOf<Map<Int, Float>>(emptyMap()) }
    
    LaunchedEffect(groups.size > 0) {
        groups.forEachIndexed { index, group ->
            if (group.size > 1) {
                repeat(3) {
                    delay(150)
                }
            }
        }
    }
    
    Canvas(modifier = modifier) {
        val cellWidth = size.width / gridSize
        val cellHeight = size.height / gridSize
        
        groups.filter { it.size > 1 }.forEach { group ->
            val minX = group.minOfOrNull { pos -> pos % gridSize } ?: 0
            val maxX = group.maxOfOrNull { pos -> pos % gridSize } ?: 0
            val minY = group.minOfOrNull { pos -> pos / gridSize } ?: 0
            val maxY = group.maxOfOrNull { pos -> pos / gridSize } ?: 0
            
            drawRect(
                color = MaterialTheme.colorScheme.primary,
                left = minX * cellWidth + 2.dp.toPx(),
                top = minY * cellHeight + 2.dp.toPx(),
                right = (maxX + 1) * cellWidth - 2.dp.toPx(),
                bottom = (maxY + 1) * cellHeight - 2.dp.toPx(),
                style = androidx.compose.ui.graphics.Stroke(width = 3.dp.toPx())
            )
        }
    }
}

/**
 * Celebration animation for puzzle completion.
 */
@Composable
fun CompletionCelebration(
    moves: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = "Puzzle Solved!",
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.primary
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Text("Moves: $moves", style = MaterialTheme.typography.bodyMedium)
}

/**
 * Tile swap animation.
 */
@Composable
fun SwapAnimation(
    fromPosition: Int,
    toPosition: Int,
    gridSize: Int,
    cellSize: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    Box(modifier = modifier.offset(x = 0.dp, y = 0.dp)) {
        // Placeholder for animated tile content
    }
}
