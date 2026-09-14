package com.hb.puzz.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
 * Fluid drag-and-drop picture board. Correctly connected tiles receive a subtle
 * accent, while newly connected tiles briefly pulse to reinforce progress.
 */
@Composable
fun PuzzleBoard(
    engine: PuzzleEngine,
    boardVersion: Int,
    image: ImageBitmap,
    connectedTileIds: Set<Int>,
    celebratingTileIds: Set<Int>,
    celebrationVersion: Int,
    onTileDropped: (fromPosition: Int, toPosition: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridSize = engine.gridSize
    val positions = remember(boardVersion) { engine.getCurrentPositions() }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline
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
                .background(surface, RoundedCornerShape(14.dp))
                .border(1.5.dp, primary.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
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
                        .background(primary.copy(alpha = 0.14f), RoundedCornerShape(5.dp))
                        .border(3.dp, primary.copy(alpha = 0.88f), RoundedCornerShape(5.dp))
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
                    val isConnected = tileId in connectedTileIds
                    val isCelebrating = tileId in celebratingTileIds
                    val celebrationPulse = remember(tileId) { Animatable(1f) }

                    LaunchedEffect(celebrationVersion, isCelebrating) {
                        if (isCelebrating) {
                            celebrationPulse.snapTo(1f)
                            celebrationPulse.animateTo(
                                1.095f,
                                animationSpec = tween(durationMillis = 120)
                            )
                            celebrationPulse.animateTo(
                                1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }

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
                    val dragScale by animateFloatAsState(
                        targetValue = if (isDragging) 1.075f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "tile-scale-$tileId"
                    )

                    val x = animatedX + if (isDragging) dragDelta.x else 0f
                    val y = animatedY + if (isDragging) dragDelta.y else 0f
                    val combinedScale = dragScale * celebrationPulse.value

                    val borderWidth = when {
                        isCelebrating -> 3.dp
                        isDragging -> 2.5.dp
                        isConnected -> 1.35.dp
                        else -> 0.45.dp
                    }
                    val borderColor = when {
                        isCelebrating -> secondary
                        isDragging -> primary
                        isConnected -> secondary.copy(alpha = 0.78f)
                        else -> outline.copy(alpha = 0.28f)
                    }

                    Canvas(
                        modifier = Modifier
                            .size(tileSize)
                            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                            .zIndex(if (isDragging || isCelebrating) 10f else 1f)
                            .graphicsLayer {
                                scaleX = combinedScale
                                scaleY = combinedScale
                                shadowElevation = when {
                                    isDragging -> 20.dp.toPx()
                                    isCelebrating -> 14.dp.toPx()
                                    isConnected -> 3.dp.toPx()
                                    else -> 0.5.dp.toPx()
                                }
                                shape = RoundedCornerShape(5.dp)
                                clip = true
                            }
                            .border(
                                width = borderWidth,
                                color = borderColor,
                                shape = RoundedCornerShape(5.dp)
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
