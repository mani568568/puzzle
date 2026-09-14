package com.hb.puzz.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
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
 * Picture puzzle board with rigid connected clusters.
 *
 * Loose tiles render independently. Once tiles become correctly connected they are rendered as
 * one clipped image fragment (one Canvas / one image draw), which removes internal tile seams.
 * Dragging any tile in that fragment moves the whole merged fragment together.
 */
@Composable
fun PuzzleBoard(
    engine: PuzzleEngine,
    boardVersion: Int,
    image: ImageBitmap,
    celebratingTileIds: Set<Int>,
    celebrationVersion: Int,
    onGroupDropped: (anchorTileId: Int, targetPosition: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridSize = engine.gridSize
    val positions = remember(boardVersion) { engine.getCurrentPositions() }
    val connectedGroups = remember(boardVersion) {
        engine.getConnectedGroups().map { it.toSet() }
    }
    val groupedTileIds = remember(connectedGroups) { connectedGroups.flatten().toSet() }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline
    val density = LocalDensity.current

    var draggingAnchorTileId by remember { mutableStateOf<Int?>(null) }
    var draggingGroupIds by remember { mutableStateOf(emptySet<Int>()) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }
    var hoverPosition by remember { mutableStateOf<Int?>(null) }

    fun resetDrag() {
        draggingAnchorTileId = null
        draggingGroupIds = emptySet()
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
                .border(1.5.dp, primary.copy(alpha = 0.48f), RoundedCornerShape(14.dp))
        ) {
            // A single shared preview footprint keeps merged pieces looking like one object.
            val anchor = draggingAnchorTileId
            val hovered = hoverPosition
            if (anchor != null && hovered != null) {
                val previewTargets = engine.getGroupMoveTargets(anchor, hovered)
                if (previewTargets != null) {
                    val previewPositions = previewTargets.values.toSet()
                    Canvas(modifier = Modifier.size(boardSize)) {
                        val mask = Path()
                        previewPositions.forEach { position ->
                            val row = position / gridSize
                            val col = position % gridSize
                            val left = col * tileSizePx
                            val top = row * tileSizePx
                            mask.addRect(Rect(left, top, left + tileSizePx, top + tileSizePx))
                        }
                        drawPath(mask, secondary.copy(alpha = 0.10f))

                        previewPositions.forEach { position ->
                            val row = position / gridSize
                            val col = position % gridSize
                            val left = col * tileSizePx
                            val top = row * tileSizePx
                            val right = left + tileSizePx
                            val bottom = top + tileSizePx
                            val stroke = 2.dp.toPx()
                            val color = primary.copy(alpha = 0.68f)

                            if (row == 0 || position - gridSize !in previewPositions) {
                                drawLine(color, Offset(left, top), Offset(right, top), stroke, StrokeCap.Round)
                            }
                            if (row == gridSize - 1 || position + gridSize !in previewPositions) {
                                drawLine(color, Offset(left, bottom), Offset(right, bottom), stroke, StrokeCap.Round)
                            }
                            if (col == 0 || position - 1 !in previewPositions) {
                                drawLine(color, Offset(left, top), Offset(left, bottom), stroke, StrokeCap.Round)
                            }
                            if (col == gridSize - 1 || position + 1 !in previewPositions) {
                                drawLine(color, Offset(right, top), Offset(right, bottom), stroke, StrokeCap.Round)
                            }
                        }
                    }
                }
            }

            // Render loose tiles independently.
            for (tileId in positions.indices) {
                if (tileId in groupedTileIds) continue
                val position = positions.indexOf(tileId)
                if (position < 0) continue

                key("tile-$tileId") {
                    val row = position / gridSize
                    val col = position % gridSize
                    val targetX = col * tileSizePx
                    val targetY = row * tileSizePx
                    val isDragging = tileId in draggingGroupIds
                    val isCelebrating = tileId in celebratingTileIds
                    val celebrationPulse = remember(tileId) { Animatable(1f) }

                    LaunchedEffect(celebrationVersion, isCelebrating) {
                        if (isCelebrating) {
                            celebrationPulse.snapTo(1f)
                            celebrationPulse.animateTo(1.07f, tween(110))
                            celebrationPulse.animateTo(
                                1f,
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }

                    val animatedX by animateFloatAsState(
                        targetValue = targetX,
                        animationSpec = tween(165, easing = FastOutSlowInEasing),
                        label = "tile-x-$tileId"
                    )
                    val animatedY by animateFloatAsState(
                        targetValue = targetY,
                        animationSpec = tween(165, easing = FastOutSlowInEasing),
                        label = "tile-y-$tileId"
                    )
                    val dragScale by animateFloatAsState(
                        targetValue = if (isDragging) 1.018f else 1f,
                        animationSpec = tween(95),
                        label = "tile-scale-$tileId"
                    )

                    // Slightly smoother than the previous heavy version, while still controlled.
                    val visualDrag = if (isDragging) {
                        Offset(dragDelta.x * 0.92f, dragDelta.y * 0.92f)
                    } else Offset.Zero

                    Canvas(
                        modifier = Modifier
                            .size(tileSize)
                            .offset {
                                IntOffset(
                                    (animatedX + visualDrag.x).roundToInt(),
                                    (animatedY + visualDrag.y).roundToInt()
                                )
                            }
                            .zIndex(if (isDragging) 20f else if (isCelebrating) 12f else 1f)
                            .graphicsLayer {
                                val scale = dragScale * celebrationPulse.value
                                scaleX = scale
                                scaleY = scale
                                shadowElevation = if (isDragging) 10.dp.toPx() else 0f
                                shape = RoundedCornerShape(5.dp)
                                clip = true
                            }
                            .border(
                                width = if (isDragging) 2.2.dp else 0.45.dp,
                                color = if (isDragging) primary else outline.copy(alpha = 0.24f),
                                shape = RoundedCornerShape(5.dp)
                            )
                            .pointerInput(tileId, position, tileSizePx, boardVersion) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingAnchorTileId = tileId
                                        draggingGroupIds = setOf(tileId)
                                        dragDelta = Offset.Zero
                                        hoverPosition = position
                                    },
                                    onDragCancel = { resetDrag() },
                                    onDragEnd = {
                                        val to = hoverPosition
                                        if (to != null && to != position) onGroupDropped(tileId, to)
                                        resetDrag()
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragDelta += amount
                                        hoverPosition = calculateHoverPosition(
                                            engine = engine,
                                            anchorTileId = tileId,
                                            fallbackPosition = position,
                                            dragDelta = dragDelta,
                                            tileSizePx = tileSizePx,
                                            gridSize = gridSize
                                        )
                                    }
                                )
                            }
                    ) {
                        drawSingleTile(image, tileId, gridSize)
                    }
                }
            }

            // Render each correctly connected group once as one seamless image fragment.
            connectedGroups.forEach { group ->
                val groupKey = group.sorted().joinToString("-")
                key("group-$groupKey") {
                    val groupPositions = group.mapNotNull { tileId ->
                        positions.indexOf(tileId).takeIf { it >= 0 }
                    }
                    val minRow = groupPositions.minOf { it / gridSize }
                    val maxRow = groupPositions.maxOf { it / gridSize }
                    val minCol = groupPositions.minOf { it % gridSize }
                    val maxCol = groupPositions.maxOf { it % gridSize }
                    val widthCells = maxCol - minCol + 1
                    val heightCells = maxRow - minRow + 1
                    val baseX = minCol * tileSizePx
                    val baseY = minRow * tileSizePx
                    val groupWidth = tileSize * widthCells
                    val groupHeight = tileSize * heightCells
                    val isDragging = group.any { it in draggingGroupIds }
                    val isCelebrating = group.any { it in celebratingTileIds }
                    val anchorTileId = draggingAnchorTileId?.takeIf { it in group }
                    val celebrationPulse = remember(groupKey) { Animatable(1f) }

                    LaunchedEffect(celebrationVersion, isCelebrating) {
                        if (isCelebrating) {
                            celebrationPulse.snapTo(1f)
                            celebrationPulse.animateTo(1.045f, tween(110))
                            celebrationPulse.animateTo(
                                1f,
                                spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }

                    val animatedX by animateFloatAsState(
                        targetValue = baseX,
                        animationSpec = tween(170, easing = FastOutSlowInEasing),
                        label = "group-x-$groupKey"
                    )
                    val animatedY by animateFloatAsState(
                        targetValue = baseY,
                        animationSpec = tween(170, easing = FastOutSlowInEasing),
                        label = "group-y-$groupKey"
                    )
                    val groupScale by animateFloatAsState(
                        targetValue = if (isDragging) 1.012f else 1f,
                        animationSpec = tween(95),
                        label = "group-scale-$groupKey"
                    )
                    val visualDrag = if (isDragging) {
                        Offset(dragDelta.x * 0.92f, dragDelta.y * 0.92f)
                    } else Offset.Zero

                    Canvas(
                        modifier = Modifier
                            .size(groupWidth, groupHeight)
                            .offset {
                                IntOffset(
                                    (animatedX + visualDrag.x).roundToInt(),
                                    (animatedY + visualDrag.y).roundToInt()
                                )
                            }
                            .zIndex(if (isDragging) 30f else if (isCelebrating) 16f else 5f)
                            .graphicsLayer {
                                val scale = groupScale * celebrationPulse.value
                                scaleX = scale
                                scaleY = scale
                                // Avoid a rectangular layer shadow around irregular merged shapes.
                                shadowElevation = 0f
                                clip = false
                            }
                            .pointerInput(groupKey, boardVersion, tileSizePx) {
                                detectDragGestures(
                                    onDragStart = { startOffset ->
                                        // Pick the tile under the finger as the anchor when possible.
                                        val localCol = floor(startOffset.x / tileSizePx).toInt().coerceIn(0, widthCells - 1)
                                        val localRow = floor(startOffset.y / tileSizePx).toInt().coerceIn(0, heightCells - 1)
                                        val boardPosition = (minRow + localRow) * gridSize + (minCol + localCol)
                                        val touchedTile = positions.getOrNull(boardPosition)
                                            ?.takeIf { it in group }
                                            ?: group.first()
                                        draggingAnchorTileId = touchedTile
                                        draggingGroupIds = group
                                        dragDelta = Offset.Zero
                                        hoverPosition = engine.getPositionOf(touchedTile)
                                    },
                                    onDragCancel = { resetDrag() },
                                    onDragEnd = {
                                        val anchorId = draggingAnchorTileId
                                        val to = hoverPosition
                                        if (anchorId != null && to != null && engine.getPositionOf(anchorId) != to) {
                                            onGroupDropped(anchorId, to)
                                        }
                                        resetDrag()
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragDelta += amount
                                        val anchorId = draggingAnchorTileId ?: group.first()
                                        hoverPosition = calculateHoverPosition(
                                            engine = engine,
                                            anchorTileId = anchorId,
                                            fallbackPosition = engine.getPositionOf(anchorId),
                                            dragDelta = dragDelta,
                                            tileSizePx = tileSizePx,
                                            gridSize = gridSize
                                        )
                                    }
                                )
                            }
                    ) {
                        val occupiedPositions = group.map { positions.indexOf(it) }.toSet()
                        val mask = Path()
                        occupiedPositions.forEach { boardPosition ->
                            val localRow = boardPosition / gridSize - minRow
                            val localCol = boardPosition % gridSize - minCol
                            val left = localCol * (size.width / widthCells)
                            val top = localRow * (size.height / heightCells)
                            val right = (localCol + 1) * (size.width / widthCells)
                            val bottom = (localRow + 1) * (size.height / heightCells)
                            mask.addRect(Rect(left, top, right, bottom))
                        }

                        // Because all tiles in a connected group preserve their original relative
                        // orientation, one source rectangle can be drawn across the whole group.
                        // Drawing once (rather than one bitmap slice per tile) removes inner seams.
                        val sourceMinRow = group.minOf { it / gridSize }
                        val sourceMaxRow = group.maxOf { it / gridSize }
                        val sourceMinCol = group.minOf { it % gridSize }
                        val sourceMaxCol = group.maxOf { it % gridSize }
                        val srcLeft = sourceMinCol * image.width / gridSize
                        val srcTop = sourceMinRow * image.height / gridSize
                        val srcRight = (sourceMaxCol + 1) * image.width / gridSize
                        val srcBottom = (sourceMaxRow + 1) * image.height / gridSize

                        clipPath(mask) {
                            drawImage(
                                image = image,
                                srcOffset = IntOffset(srcLeft, srcTop),
                                srcSize = IntSize(srcRight - srcLeft, srcBottom - srcTop),
                                dstOffset = IntOffset.Zero,
                                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                                filterQuality = FilterQuality.High
                            )
                        }

                        // Shared outer contour only — no internal grid lines at all.
                        val glowWidth = when {
                            isCelebrating -> 7.dp.toPx()
                            isDragging -> 5.5.dp.toPx()
                            else -> 4.dp.toPx()
                        }
                        val edgeWidth = when {
                            isCelebrating -> 3.dp.toPx()
                            isDragging -> 2.4.dp.toPx()
                            else -> 1.7.dp.toPx()
                        }
                        val glowColor = when {
                            isCelebrating -> secondary.copy(alpha = 0.48f)
                            isDragging -> primary.copy(alpha = 0.30f)
                            else -> secondary.copy(alpha = 0.20f)
                        }
                        val edgeColor = when {
                            isCelebrating -> secondary
                            isDragging -> primary
                            else -> primary.copy(alpha = 0.88f)
                        }

                        occupiedPositions.forEach { boardPosition ->
                            val boardRow = boardPosition / gridSize
                            val boardCol = boardPosition % gridSize
                            val localRow = boardRow - minRow
                            val localCol = boardCol - minCol
                            val cellW = size.width / widthCells
                            val cellH = size.height / heightCells
                            val left = localCol * cellW
                            val top = localRow * cellH
                            val right = left + cellW
                            val bottom = top + cellH

                            fun outerEdge(start: Offset, end: Offset) {
                                drawLine(glowColor, start, end, glowWidth, StrokeCap.Round)
                                drawLine(edgeColor, start, end, edgeWidth, StrokeCap.Round)
                            }

                            if (boardRow == 0 || boardPosition - gridSize !in occupiedPositions) {
                                outerEdge(Offset(left, top), Offset(right, top))
                            }
                            if (boardRow == gridSize - 1 || boardPosition + gridSize !in occupiedPositions) {
                                outerEdge(Offset(left, bottom), Offset(right, bottom))
                            }
                            if (boardCol == 0 || boardPosition - 1 !in occupiedPositions) {
                                outerEdge(Offset(left, top), Offset(left, bottom))
                            }
                            if (boardCol == gridSize - 1 || boardPosition + 1 !in occupiedPositions) {
                                outerEdge(Offset(right, top), Offset(right, bottom))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun calculateHoverPosition(
    engine: PuzzleEngine,
    anchorTileId: Int,
    fallbackPosition: Int,
    dragDelta: Offset,
    tileSizePx: Float,
    gridSize: Int
): Int? {
    val anchorPosition = engine.getPositionOf(anchorTileId).takeIf { it >= 0 } ?: fallbackPosition
    if (anchorPosition < 0) return null

    val startRow = anchorPosition / gridSize
    val startCol = anchorPosition % gridSize
    val centerX = startCol * tileSizePx + tileSizePx / 2f + dragDelta.x
    val centerY = startRow * tileSizePx + tileSizePx / 2f + dragDelta.y
    val boardPixels = tileSizePx * gridSize

    if (centerX < 0f || centerY < 0f || centerX >= boardPixels || centerY >= boardPixels) {
        return null
    }

    val targetCol = floor(centerX / tileSizePx).toInt()
    val targetRow = floor(centerY / tileSizePx).toInt()
    val candidate = targetRow * gridSize + targetCol
    return candidate.takeIf { engine.getGroupMoveTargets(anchorTileId, it) != null }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSingleTile(
    image: ImageBitmap,
    tileId: Int,
    gridSize: Int
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
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        filterQuality = FilterQuality.High
    )
}
