package com.hb.puzz.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hb.puzz.domain.PuzzleEngine
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Drag-and-drop picture board. Each source tile is its own composable so swaps
 * can spring smoothly into place instead of redrawing the whole image at once.
 */
@Composable
fun PuzzleBoard(
    engine: PuzzleEngine,
    boardVersion: Int,
    image: ImageBitmap,
    onTileDropped: (fromPosition: Int, toPosition: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridSize = engine.gridSize
    val positions = remember(boardVersion) { engine.getCurrentPositions() }
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val density = LocalDensity.current

    var draggingTileId by remember { mutableStateOf<Int?>(null) }
    var dragFromPosition by remember { mutableStateOf<Int?>(null) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    var hoverPosition by remember { mutableStateOf<Int?>(null) }

    fun resetDrag() {
        draggingTileId = null
        dragFromPosition = null
        dragDelta = Offset.Zero
        hoverPosition = null
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val boardSize = minOf(maxWidth, maxHeight)
        val tileSize = boardSize / gridSize
        val tileSizePx = with(density) { tileSize.toPx() }

        Box(
            modifier = Modifier
                .size(boardSize)
                .background(surface, RoundedCornerShape(16.dp))
                .border(2.dp, primary.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
        ) {
            val hovered = hoverPosition
            val origin = dragFromPosition
            if (hovered != null && origin != null && hovered != origin) {
                val row = hovered / gridSize
                val col = hovered % gridSize
                Box(
                    modifier = Modifier
                        .size(tileSize)
                        .offset(x = tileSize * col, y = tileSize * row)
                        .background(primary.copy(alpha = 0.12f), RoundedCornerShape(7.dp))
                        .border(3.dp, primary.copy(alpha = 0.8f), RoundedCornerShape(7.dp))
                )
            }

            for (tileId in positions.indices) {
                val position = positions.indexOf(tileId)
                if (position < 0) continue
                key(tileId) {
                    val row = position / gridSize
                    val col = position % gridSize
                    val targetX = col * tileSizePx
                    val targetY = row * tileSizePx
                    val isDragging = draggingTileId == tileId

                    val animatedX by animateFloatAsState(
                        targetValue = targetX,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "tile-x-$tileId"
                    )
                    val animatedY by animateFloatAsState(
                        targetValue = targetY,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "tile-y-$tileId"
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (isDragging) 1.06f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "tile-scale-$tileId"
                    )

                    val x = animatedX + if (isDragging) dragDelta.x else 0f
                    val y = animatedY + if (isDragging) dragDelta.y else 0f

                    Canvas(
                        modifier = Modifier
                            .size(tileSize)
                            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                            .zIndex(if (isDragging) 10f else 1f)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                shadowElevation = if (isDragging) 18.dp.toPx() else 1.dp.toPx()
                                shape = RoundedCornerShape(7.dp)
                                clip = true
                            }
                            .border(
                                width = if (isDragging) 2.dp else 0.7.dp,
                                color = if (isDragging) primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(7.dp)
                            )
                            .pointerInput(tileId, position, tileSizePx, boardVersion) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingTileId = tileId
                                        dragFromPosition = position
                                        dragDelta = Offset.Zero
                                        hoverPosition = position
                                    },
                                    onDragCancel = { resetDrag() },
                                    onDragEnd = {
                                        val from = dragFromPosition
                                        val to = hoverPosition
                                        if (from != null && to != null && from != to) {
                                            onTileDropped(from, to)
                                        }
                                        resetDrag()
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragDelta += amount

                                        val start = dragFromPosition ?: position
                                        val startRow = start / gridSize
                                        val startCol = start % gridSize
                                        val centerX = startCol * tileSizePx + tileSizePx / 2f + dragDelta.x
                                        val centerY = startRow * tileSizePx + tileSizePx / 2f + dragDelta.y
                                        val boardPixels = tileSizePx * gridSize
                                        hoverPosition = if (
                                            centerX < 0f || centerY < 0f ||
                                            centerX >= boardPixels || centerY >= boardPixels
                                        ) {
                                            null
                                        } else {
                                            val targetCol = floor(centerX / tileSizePx).toInt()
                                            val targetRow = floor(centerY / tileSizePx).toInt()
                                            targetRow * gridSize + targetCol
                                        }
                                    }
                                )
                            }
                    ) {
                        val sourceRow = tileId / gridSize
                        val sourceCol = tileId % gridSize
                        val srcLeft = sourceCol * image.width / gridSize
                        val srcTop = sourceRow * image.height / gridSize
                        val srcRight = (sourceCol + 1) * image.width / gridSize
                        val srcBottom = (sourceRow + 1) * image.height / gridSize

                        drawImage(
                            image = image,
                            srcOffset = IntOffset(srcLeft, srcTop),
                            srcSize = IntSize(srcRight - srcLeft, srcBottom - srcTop),
                            dstOffset = IntOffset(0, 0),
                            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                            filterQuality = FilterQuality.Medium
                        )
                    }
                }
            }
        }
    }
}
