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
import androidx.compose.ui.graphics.RectangleShape
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
import androidx.compose.ui.graphics.StrokeCap
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
 * Fluid drag-and-drop picture board with rigid connected clusters.
 *
 * Correct neighbors visually fuse: their internal seam disappears and a shared glowing
 * outline is drawn only around the outside of the merged shape. Dragging any tile in a
 * connected cluster moves the whole cluster together.
 */
@Composable
fun PuzzleBoard(
    engine: PuzzleEngine,
    boardVersion: Int,
    image: ImageBitmap,
    connectedTileIds: Set<Int>,
    celebratingTileIds: Set<Int>,
    celebrationVersion: Int,
    onGroupDropped: (anchorTileId: Int, targetPosition: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridSize = engine.gridSize
    val positions = remember(boardVersion) { engine.getCurrentPositions() }
    val groupsByTile = remember(boardVersion) {
        buildMap<Int, Set<Int>> {
            engine.getConnectedGroups().forEach { group ->
                val set = group.toSet()
                group.forEach { put(it, set) }
            }
        }
    }
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
            // Preview the entire rigid cluster footprint, not just one destination cell.
            val anchor = draggingAnchorTileId
            val hovered = hoverPosition
            if (anchor != null && hovered != null) {
                val previewTargets = engine.getGroupMoveTargets(anchor, hovered)
                previewTargets?.values?.forEach { previewPosition ->
                    val row = previewPosition / gridSize
                    val col = previewPosition % gridSize
                    Box(
                        modifier = Modifier
                            .size(tileSize)
                            .offset(x = tileSize * col, y = tileSize * row)
                            .background(primary.copy(alpha = 0.09f))
                            .border(1.4.dp, primary.copy(alpha = 0.48f))
                    )
                }
            }

            for (tileId in positions.indices) {
                val position = positions.indexOf(tileId)
                if (position < 0) continue

                key(tileId) {
                    val row = position / gridSize
                    val col = position % gridSize
                    val targetX = col * tileSizePx
                    val targetY = row * tileSizePx
                    val tileGroup = groupsByTile[tileId] ?: setOf(tileId)
                    val isMerged = tileGroup.size > 1
                    val isDraggingGroup = tileId in draggingGroupIds
                    val isDraggingAnchor = draggingAnchorTileId == tileId
                    val isConnected = tileId in connectedTileIds
                    val isCelebrating = tileId in celebratingTileIds
                    val celebrationPulse = remember(tileId) { Animatable(1f) }

                    LaunchedEffect(celebrationVersion, isCelebrating) {
                        if (isCelebrating) {
                            celebrationPulse.snapTo(1f)
                            celebrationPulse.animateTo(1.075f, tween(durationMillis = 115))
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
                        targetValue = if (isDraggingGroup) 1.045f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "tile-scale-$tileId"
                    )

                    val x = animatedX + if (isDraggingGroup) dragDelta.x else 0f
                    val y = animatedY + if (isDraggingGroup) dragDelta.y else 0f
                    val combinedScale = dragScale * celebrationPulse.value

                    val groupPositionSet = remember(boardVersion, tileGroup) {
                        tileGroup.mapNotNull { groupTile ->
                            positions.indexOf(groupTile).takeIf { it >= 0 }
                        }.toSet()
                    }
                    val hasLeft = col > 0 && (position - 1) in groupPositionSet
                    val hasRight = col + 1 < gridSize && (position + 1) in groupPositionSet
                    val hasTop = row > 0 && (position - gridSize) in groupPositionSet
                    val hasBottom = row + 1 < gridSize && (position + gridSize) in groupPositionSet

                    val tileShape = if (isMerged) RectangleShape else RoundedCornerShape(5.dp)

                    Canvas(
                        modifier = Modifier
                            .size(tileSize)
                            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                            .zIndex(
                                when {
                                    isDraggingGroup -> 20f
                                    isCelebrating -> 12f
                                    isMerged -> 4f
                                    else -> 1f
                                }
                            )
                            .graphicsLayer {
                                scaleX = combinedScale
                                scaleY = combinedScale
                                shadowElevation = when {
                                    isDraggingGroup -> 18.dp.toPx()
                                    isCelebrating -> 9.dp.toPx()
                                    else -> 0f
                                }
                                shape = tileShape
                                clip = true
                            }
                            .then(
                                if (!isMerged) {
                                    Modifier.border(
                                        width = if (isDraggingAnchor) 2.4.dp else 0.45.dp,
                                        color = if (isDraggingAnchor) primary else outline.copy(alpha = 0.26f),
                                        shape = RoundedCornerShape(5.dp)
                                    )
                                } else Modifier
                            )
                            .pointerInput(tileId, position, tileSizePx, boardVersion) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingAnchorTileId = tileId
                                        draggingGroupIds = engine.getGroupForTile(tileId)
                                        dragDelta = Offset.Zero
                                        hoverPosition = position
                                    },
                                    onDragCancel = { resetDrag() },
                                    onDragEnd = {
                                        val anchorTile = draggingAnchorTileId
                                        val to = hoverPosition
                                        if (anchorTile != null && to != null && engine.getPositionOf(anchorTile) != to) {
                                            onGroupDropped(anchorTile, to)
                                        }
                                        resetDrag()
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragDelta += amount

                                        val anchorPosition = draggingAnchorTileId
                                            ?.let { engine.getPositionOf(it) }
                                            ?.takeIf { it >= 0 }
                                            ?: position
                                        val startRow = anchorPosition / gridSize
                                        val startCol = anchorPosition % gridSize
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
                                            val candidate = targetRow * gridSize + targetCol
                                            if (draggingAnchorTileId?.let {
                                                    engine.getGroupMoveTargets(it, candidate) != null
                                                } == true
                                            ) candidate else null
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
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                            filterQuality = FilterQuality.Medium
                        )

                        if (isMerged) {
                            // Two-layer shared outline: soft peach glow + crisp teal/peach edge.
                            // Internal edges are intentionally omitted so the block reads as one image.
                            val glowWidth = if (isCelebrating) 8.dp.toPx() else 5.dp.toPx()
                            val edgeWidth = if (isCelebrating) 3.2.dp.toPx() else 1.9.dp.toPx()
                            val glowColor = secondary.copy(alpha = if (isCelebrating) 0.52f else 0.24f)
                            val edgeColor = if (isCelebrating) secondary else primary.copy(alpha = 0.92f)
                            val glowInset = glowWidth / 2f
                            val edgeInset = edgeWidth / 2f

                            fun edge(start: Offset, end: Offset, insetStart: Offset, insetEnd: Offset) {
                                drawLine(glowColor, start + insetStart, end + insetEnd, glowWidth, StrokeCap.Round)
                                drawLine(edgeColor, start + insetStart * (edgeInset / glowInset), end + insetEnd * (edgeInset / glowInset), edgeWidth, StrokeCap.Round)
                            }

                            if (!hasTop) {
                                edge(
                                    Offset(0f, 0f), Offset(size.width, 0f),
                                    Offset(0f, glowInset), Offset(0f, glowInset)
                                )
                            }
                            if (!hasBottom) {
                                edge(
                                    Offset(0f, size.height), Offset(size.width, size.height),
                                    Offset(0f, -glowInset), Offset(0f, -glowInset)
                                )
                            }
                            if (!hasLeft) {
                                edge(
                                    Offset(0f, 0f), Offset(0f, size.height),
                                    Offset(glowInset, 0f), Offset(glowInset, 0f)
                                )
                            }
                            if (!hasRight) {
                                edge(
                                    Offset(size.width, 0f), Offset(size.width, size.height),
                                    Offset(-glowInset, 0f), Offset(-glowInset, 0f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
