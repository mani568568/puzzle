package com.hb.puzz.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
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
    val gridBorder = Color(0xFFC9A064)
    val tileDivider = Color.White
    val dragAccent = Color(0xFF67C9D7)
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
        val boardWidth = maxWidth
        val boardHeight = maxHeight
        val tileWidth = boardWidth / gridSize
        val tileHeight = boardHeight / gridSize
        val tileWidthPx = with(density) { tileWidth.toPx() }
        val tileHeightPx = with(density) { tileHeight.toPx() }

        Box(
            modifier = Modifier
                .width(boardWidth)
                .height(boardHeight)
                .background(Color(0xFFFFFBF5), RoundedCornerShape(10.dp))
                .border(2.5.dp, gridBorder, RoundedCornerShape(10.dp))
        ) {
            // A single shared preview footprint keeps merged pieces looking like one object.
            val anchor = draggingAnchorTileId
            val hovered = hoverPosition
            if (anchor != null && hovered != null) {
                val previewTargets = engine.getGroupMoveTargets(anchor, hovered)
                if (previewTargets != null) {
                    val previewPositions = previewTargets.values.toSet()
                    Canvas(modifier = Modifier.width(boardWidth).height(boardHeight)) {
                        val mask = Path()
                        previewPositions.forEach { position ->
                            val row = position / gridSize
                            val col = position % gridSize
                            val left = col * tileWidthPx
                            val top = row * tileHeightPx
                            mask.addRect(Rect(left, top, left + tileWidthPx, top + tileHeightPx))
                        }
                        drawPath(mask, dragAccent.copy(alpha = 0.10f))

                        previewPositions.forEach { position ->
                            val row = position / gridSize
                            val col = position % gridSize
                            val left = col * tileWidthPx
                            val top = row * tileHeightPx
                            val right = left + tileWidthPx
                            val bottom = top + tileHeightPx
                            val stroke = 2.dp.toPx()
                            val color = dragAccent.copy(alpha = 0.88f)

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
                    val targetX = col * tileWidthPx
                    val targetY = row * tileHeightPx
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
                        animationSpec = tween(235, easing = FastOutSlowInEasing),
                        label = "tile-x-$tileId"
                    )
                    val animatedY by animateFloatAsState(
                        targetValue = targetY,
                        animationSpec = tween(235, easing = FastOutSlowInEasing),
                        label = "tile-y-$tileId"
                    )
                    val dragScale by animateFloatAsState(
                        targetValue = if (isDragging) 1.018f else 1f,
                        animationSpec = tween(95),
                        label = "tile-scale-$tileId"
                    )

                    // Slightly smoother than the previous heavy version, while still controlled.
                    val visualDrag = if (isDragging) {
                        Offset(dragDelta.x * 0.86f, dragDelta.y * 0.86f)
                    } else Offset.Zero

                    Canvas(
                        modifier = Modifier
                            .width(tileWidth)
                            .height(tileHeight)
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
                                shape = RectangleShape
                                clip = true
                            }
                            .then(
                                if (isDragging) {
                                    Modifier.border(2.dp, dragAccent, RectangleShape)
                                } else {
                                    Modifier
                                }
                            )
                            .pointerInput(tileId, position, tileWidthPx, tileHeightPx, boardVersion) {
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
                                            tileWidthPx = tileWidthPx,
                                            tileHeightPx = tileHeightPx,
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

            // Draw one shared white grid after the loose tiles. Using a single overlay avoids
            // doubled/misaligned borders from neighboring tiles. Correctly merged groups are
            // rendered after this overlay, so their internal white seams disappear naturally.
            Canvas(
                modifier = Modifier
                    .width(boardWidth)
                    .height(boardHeight)
                    .zIndex(3f)
            ) {
                val lineWidth = 1.35.dp.toPx()
                val divider = tileDivider.copy(alpha = 0.98f)
                for (column in 1 until gridSize) {
                    val x = column * tileWidthPx
                    drawLine(
                        color = divider,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = lineWidth
                    )
                }
                for (row in 1 until gridSize) {
                    val y = row * tileHeightPx
                    drawLine(
                        color = divider,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = lineWidth
                    )
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
                    val baseX = minCol * tileWidthPx
                    val baseY = minRow * tileHeightPx
                    val groupWidth = tileWidth * widthCells
                    val groupHeight = tileHeight * heightCells
                    val isDragging = group.any { it in draggingGroupIds }
                    val isCelebrating = group.any { it in celebratingTileIds }
                    val anchorTileId = draggingAnchorTileId?.takeIf { it in group }
                    val celebrationPulse = remember(groupKey) { Animatable(1f) }
                    val pixieDustProgress = remember(groupKey) { Animatable(1f) }

                    LaunchedEffect(celebrationVersion, isCelebrating) {
                        if (isCelebrating) {
                            pixieDustProgress.snapTo(0f)
                            launch {
                                pixieDustProgress.animateTo(
                                    1f,
                                    tween(durationMillis = 1_280, easing = LinearEasing)
                                )
                            }
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
                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                        label = "group-x-$groupKey"
                    )
                    val animatedY by animateFloatAsState(
                        targetValue = baseY,
                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                        label = "group-y-$groupKey"
                    )
                    val groupScale by animateFloatAsState(
                        targetValue = if (isDragging) 1.012f else 1f,
                        animationSpec = tween(95),
                        label = "group-scale-$groupKey"
                    )
                    val visualDrag = if (isDragging) {
                        Offset(dragDelta.x * 0.86f, dragDelta.y * 0.86f)
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
                            isCelebrating -> dragAccent.copy(alpha = 0.52f)
                            isDragging -> dragAccent.copy(alpha = 0.34f)
                            else -> gridBorder.copy(alpha = 0.18f)
                        }
                        val edgeColor = when {
                            isCelebrating -> dragAccent
                            isDragging -> dragAccent
                            else -> gridBorder.copy(alpha = 0.96f)
                        }

                        // Build the outer perimeter once so the stable border and the magical
                        // merge trail are rendered from exactly the same geometry. The third value
                        // is a small inward normal, keeping sparkles inside the cluster bounds.
                        val outerEdges = mutableListOf<Triple<Offset, Offset, Offset>>()
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

                            if (boardRow == 0 || boardPosition - gridSize !in occupiedPositions) {
                                outerEdges += Triple(Offset(left, top), Offset(right, top), Offset(0f, 1f))
                            }
                            if (boardRow == gridSize - 1 || boardPosition + gridSize !in occupiedPositions) {
                                outerEdges += Triple(Offset(left, bottom), Offset(right, bottom), Offset(0f, -1f))
                            }
                            if (boardCol == 0 || boardPosition - 1 !in occupiedPositions) {
                                outerEdges += Triple(Offset(left, top), Offset(left, bottom), Offset(1f, 0f))
                            }
                            if (boardCol == gridSize - 1 || boardPosition + 1 !in occupiedPositions) {
                                outerEdges += Triple(Offset(right, top), Offset(right, bottom), Offset(-1f, 0f))
                            }
                        }

                        outerEdges.forEach { (start, end, _) ->
                            drawLine(glowColor, start, end, glowWidth, StrokeCap.Round)
                            drawLine(edgeColor, start, end, edgeWidth, StrokeCap.Round)
                        }

                        // Pixie-dust merge moment: a warm-gold shimmer races around every exposed
                        // edge while tiny aqua/white/gold particles twinkle just inside the block.
                        // It only runs for newly-created/expanded groups; the normal merged outline
                        // remains quiet after the effect settles.
                        if (isCelebrating) {
                            val progress = pixieDustProgress.value.coerceIn(0f, 1f)
                            val envelope = sin(progress.toDouble() * PI).toFloat().coerceAtLeast(0f)
                            val shimmer = Color(0xFFFFD66B).copy(alpha = 0.28f + envelope * 0.62f)
                            val sparkleColors = listOf(
                                Color(0xFFFFE58A),
                                Color(0xFF8FE7E8),
                                Color.White,
                                Color(0xFFFFC7E7)
                            )
                            val inward = 3.2.dp.toPx()

                            outerEdges.forEachIndexed { edgeIndex, edge ->
                                val (start, end, normal) = edge
                                drawLine(
                                    color = shimmer,
                                    start = start,
                                    end = end,
                                    strokeWidth = edgeWidth + 1.7.dp.toPx(),
                                    cap = StrokeCap.Round
                                )

                                repeat(4) { particleIndex ->
                                    val phase = (progress * 1.85f + edgeIndex * 0.173f + particleIndex * 0.229f) % 1f
                                    val dx = end.x - start.x
                                    val dy = end.y - start.y
                                    val wave = sin((progress * 9f + edgeIndex + particleIndex * 0.7f).toDouble() * PI).toFloat()
                                    val center = Offset(
                                        x = start.x + dx * phase + normal.x * inward * (0.65f + 0.35f * wave),
                                        y = start.y + dy * phase + normal.y * inward * (0.65f + 0.35f * wave)
                                    )
                                    val twinkle = (0.58f + 0.42f * sin((progress * 13f + particleIndex).toDouble() * PI).toFloat()).coerceIn(0.15f, 1f)
                                    val radius = (1.25f + (particleIndex % 3) * 0.72f) * density.density
                                    val color = sparkleColors[(edgeIndex + particleIndex) % sparkleColors.size]
                                    drawCircle(
                                        color = color.copy(alpha = envelope * twinkle),
                                        radius = radius,
                                        center = center
                                    )

                                    if ((edgeIndex + particleIndex) % 3 == 0 && envelope > 0.18f) {
                                        val ray = radius * 1.8f
                                        drawLine(
                                            Color.White.copy(alpha = envelope * 0.82f),
                                            Offset(center.x - ray, center.y),
                                            Offset(center.x + ray, center.y),
                                            0.8.dp.toPx(),
                                            StrokeCap.Round
                                        )
                                        drawLine(
                                            Color.White.copy(alpha = envelope * 0.82f),
                                            Offset(center.x, center.y - ray),
                                            Offset(center.x, center.y + ray),
                                            0.8.dp.toPx(),
                                            StrokeCap.Round
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Important: keep gesture hit-testing on the *actual occupied cells*, not on
                    // the rectangular bounding box of the merged artwork. An L/T-shaped merged
                    // fragment can contain transparent holes inside its bounds; a large rectangular
                    // pointer target would otherwise sit on top of loose tiles in those holes and
                    // make them feel impossible to pick up or drag across the merged fragment.
                    group.forEach { hitTileId ->
                        val hitBoardPosition = positions.indexOf(hitTileId)
                        if (hitBoardPosition >= 0) {
                            val hitBoardRow = hitBoardPosition / gridSize
                            val hitBoardCol = hitBoardPosition % gridSize
                            val localHitRow = hitBoardRow - minRow
                            val localHitCol = hitBoardCol - minCol
                            val hitX = animatedX + localHitCol * tileWidthPx + visualDrag.x
                            val hitY = animatedY + localHitRow * tileHeightPx + visualDrag.y

                            Canvas(
                                modifier = Modifier
                                    .width(tileWidth)
                            .height(tileHeight)
                                    .offset {
                                        IntOffset(hitX.roundToInt(), hitY.roundToInt())
                                    }
                                    .zIndex(if (isDragging) 40f else 18f)
                                    .pointerInput(groupKey, hitTileId, boardVersion, tileWidthPx, tileHeightPx) {
                                        detectDragGestures(
                                            onDragStart = {
                                                draggingAnchorTileId = hitTileId
                                                draggingGroupIds = group
                                                dragDelta = Offset.Zero
                                                hoverPosition = engine.getPositionOf(hitTileId)
                                            },
                                            onDragCancel = { resetDrag() },
                                            onDragEnd = {
                                                val anchorId = draggingAnchorTileId
                                                val to = hoverPosition
                                                if (
                                                    anchorId != null &&
                                                    to != null &&
                                                    engine.getPositionOf(anchorId) != to
                                                ) {
                                                    onGroupDropped(anchorId, to)
                                                }
                                                resetDrag()
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragDelta += amount
                                                val anchorId = draggingAnchorTileId ?: hitTileId
                                                hoverPosition = calculateHoverPosition(
                                                    engine = engine,
                                                    anchorTileId = anchorId,
                                                    fallbackPosition = engine.getPositionOf(anchorId),
                                                    dragDelta = dragDelta,
                                                    tileWidthPx = tileWidthPx,
                                                    tileHeightPx = tileHeightPx,
                                                    gridSize = gridSize
                                                )
                                            }
                                        )
                                    }
                            ) { /* Transparent hit target for this occupied group cell only. */ }
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
    tileWidthPx: Float,
    tileHeightPx: Float,
    gridSize: Int
): Int? {
    val anchorPosition = engine.getPositionOf(anchorTileId).takeIf { it >= 0 } ?: fallbackPosition
    if (anchorPosition < 0) return null

    val startRow = anchorPosition / gridSize
    val startCol = anchorPosition % gridSize
    val centerX = startCol * tileWidthPx + tileWidthPx / 2f + dragDelta.x
    val centerY = startRow * tileHeightPx + tileHeightPx / 2f + dragDelta.y
    val boardWidthPx = tileWidthPx * gridSize
    val boardHeightPx = tileHeightPx * gridSize

    if (centerX < 0f || centerY < 0f || centerX >= boardWidthPx || centerY >= boardHeightPx) {
        return null
    }

    val targetCol = floor(centerX / tileWidthPx).toInt()
    val targetRow = floor(centerY / tileHeightPx).toInt()
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
