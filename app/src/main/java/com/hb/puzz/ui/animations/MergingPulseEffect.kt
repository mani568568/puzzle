package com.hb.puzz.ui.animations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Optional visual outline around connected picture-tile groups. */
@Composable
fun MergingPulseEffect(
    groups: List<List<Int>>,
    gridSize: Int,
    cellSize: Dp,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val cellWidth = size.width / gridSize
        val cellHeight = size.height / gridSize

        groups.filter { it.size > 1 }.forEach { group ->
            val minX = group.minOf { it % gridSize }
            val maxX = group.maxOf { it % gridSize }
            val minY = group.minOf { it / gridSize }
            val maxY = group.maxOf { it / gridSize }

            drawRect(
                color = primary,
                topLeft = Offset(
                    minX * cellWidth + 2.dp.toPx(),
                    minY * cellHeight + 2.dp.toPx()
                ),
                size = Size(
                    (maxX - minX + 1) * cellWidth - 4.dp.toPx(),
                    (maxY - minY + 1) * cellHeight - 4.dp.toPx()
                ),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

@Composable
fun CompletionCelebration(moves: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Puzzle Solved!",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Moves: $moves", style = MaterialTheme.typography.bodyMedium)
    }
}

/** Placeholder hook for a future animated swap transition. */
@Composable
fun SwapAnimation(
    fromPosition: Int,
    toPosition: Int,
    gridSize: Int,
    cellSize: Dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.offset(x = 0.dp, y = 0.dp))
}
