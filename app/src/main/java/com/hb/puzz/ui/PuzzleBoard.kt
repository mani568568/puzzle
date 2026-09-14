package com.hb.puzz.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.hb.puzz.domain.PuzzleEngine
import com.hb.puzz.ui.images.ImageAssets

/**
 * Draws the real level artwork sliced into grid tiles. The PuzzleEngine decides
 * which source tile is displayed at each board position.
 */
@Composable
fun PuzzleBoard(
    engine: PuzzleEngine,
    levelId: Int,
    onTileSwapped: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val gridSize = engine.gridSize
    val image = remember(levelId) { ImageAssets.loadBitmap(context, levelId).asImageBitmap() }
    var selectedPosition by remember(levelId, gridSize) { mutableStateOf<Int?>(null) }
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val boardSize = minOf(maxWidth, maxHeight)
        val tileSize = boardSize / gridSize

        Box(
            modifier = Modifier
                .size(boardSize)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .border(2.dp, primary, RoundedCornerShape(14.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellWidth = size.width / gridSize
                val cellHeight = size.height / gridSize
                val srcWidth = image.width / gridSize
                val srcHeight = image.height / gridSize

                for (position in 0 until engine.getTotalTiles()) {
                    val destinationRow = position / gridSize
                    val destinationCol = position % gridSize
                    val tileId = engine.getTileAt(position)
                    val sourceRow = tileId / gridSize
                    val sourceCol = tileId % gridSize

                    drawImage(
                        image = image,
                        srcOffset = IntOffset(sourceCol * srcWidth, sourceRow * srcHeight),
                        srcSize = IntSize(srcWidth, srcHeight),
                        dstOffset = IntOffset(
                            (destinationCol * cellWidth).toInt(),
                            (destinationRow * cellHeight).toInt()
                        ),
                        dstSize = IntSize(
                            (cellWidth + 1f).toInt(),
                            (cellHeight + 1f).toInt()
                        ),
                        filterQuality = FilterQuality.Medium
                    )
                }

                for (index in 1 until gridSize) {
                    val x = index * cellWidth
                    val y = index * cellHeight
                    drawLine(outline.copy(alpha = 0.7f), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx(), StrokeCap.Square)
                    drawLine(outline.copy(alpha = 0.7f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx(), StrokeCap.Square)
                }

                selectedPosition?.let { selected ->
                    val row = selected / gridSize
                    val col = selected % gridSize
                    drawRect(
                        color = primary,
                        topLeft = Offset(col * cellWidth + 2.dp.toPx(), row * cellHeight + 2.dp.toPx()),
                        size = Size(cellWidth - 4.dp.toPx(), cellHeight - 4.dp.toPx()),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
            }

            for (position in 0 until engine.getTotalTiles()) {
                val row = position / gridSize
                val col = position % gridSize
                Box(
                    modifier = Modifier
                        .size(tileSize)
                        .offset(x = tileSize * col, y = tileSize * row)
                        .clickable {
                            val first = selectedPosition
                            when {
                                first == null -> selectedPosition = position
                                first == position -> selectedPosition = null
                                else -> {
                                    onTileSwapped(first, position)
                                    selectedPosition = null
                                }
                            }
                        }
                )
            }
        }
    }
}
