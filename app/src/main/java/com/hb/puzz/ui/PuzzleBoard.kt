package com.hb.puzz.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
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
import com.hb.puzz.domain.mergeContours
import com.hb.puzz.domain.MergeMotion
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

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
    modifier: Modifier = Modifier,
    inputEnabled: Boolean = true
) {
    val gridSize = engine.gridSize
    val positions = remember(boardVersion) { engine.getCurrentPositions() }
    val connectedGroups = remember(boardVersion) {
        engine.getConnectedGroups().map { it.toSet() }
    }
    val groupedTileIds = remember(connectedGroups) { connectedGroups.flatten().toSet() }

    val surface = MaterialTheme.colorScheme.surface
    val gridBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
    val tileDivider = MaterialTheme.colorScheme.surface
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
                .background(surface, RoundedCornerShape(10.dp))
                .border(1.dp, gridBorder, RoundedCornerShape(10.dp))
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
                    val animatedX by animateFloatAsState(
                        targetValue = targetX,
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        label = "tile-x-$tileId"
                    )
                    val animatedY by animateFloatAsState(
                        targetValue = targetY,
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        label = "tile-y-$tileId"
                    )
                    val dragScale by animateFloatAsState(
                        targetValue = if (isDragging) 1.018f else 1f,
                        animationSpec = tween(160, easing = FastOutSlowInEasing),
                        label = "tile-scale-$tileId"
                    )

                    // Slightly smoother than the previous heavy version, while still controlled.
                    val visualDrag = if (isDragging) {
                        Offset(dragDelta.x, dragDelta.y)
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
                            .zIndex(if (isDragging) 20f else 1f)
                            .graphicsLayer {
                                val scale = dragScale
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
                            .pointerInput(tileId, position, tileWidthPx, tileHeightPx, boardVersion, inputEnabled) {
                                if (inputEnabled) detectDragGestures(
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
                    val mergeProgress = remember(groupKey) { Animatable(1f) }
                    LaunchedEffect(celebrationVersion, isCelebrating) {
                        if (isCelebrating) {
                            mergeProgress.snapTo(0f)
                            mergeProgress.animateTo(1f,
                                tween(MergeMotion.DURATION_MILLIS, easing = LinearEasing))
                        } else {
                            mergeProgress.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
                        }
                    }
                    // Build continuous contours only when geometry changes, never per frame.
                    val contourPaths = remember(groupPositions, tileWidthPx, tileHeightPx) {
                        mergeContours(groupPositions.toSet(), gridSize).map { corners ->
                            Path().apply {
                                corners.forEachIndexed { index, corner ->
                                    val x = (corner.x - minCol) * tileWidthPx
                                    val y = (corner.y - minRow) * tileHeightPx
                                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                                }
                                close()
                            }
                        }
                    }
                    val contourMeasures = remember(contourPaths) {
                        contourPaths.map { path -> PathMeasure().apply { setPath(path, true) } }
                    }
                    val perimeterLength = remember(contourMeasures) { contourMeasures.sumOf { it.length.toDouble() }.toFloat() }

                    val animatedX by animateFloatAsState(
                        targetValue = baseX,
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        label = "group-x-$groupKey"
                    )
                    val animatedY by animateFloatAsState(
                        targetValue = baseY,
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        label = "group-y-$groupKey"
                    )
                    val groupScale by animateFloatAsState(
                        targetValue = if (isDragging) 1.012f else 1f,
                        animationSpec = tween(160, easing = FastOutSlowInEasing),
                        label = "group-scale-$groupKey"
                    )
                    val visualDrag = if (isDragging) {
                        Offset(dragDelta.x, dragDelta.y)
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
                            .zIndex(if (isDragging) 30f else if (mergeProgress.value < 1f) 16f else 5f)
                            .graphicsLayer {
                                val lift = mergeLift(mergeProgress.value)
                                val scale = groupScale * (1f + lift * 0.012f)
                                scaleX = scale
                                scaleY = scale
                                translationY = -4.dp.toPx() * lift
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

                        val lift = mergeLift(mergeProgress.value)
                        if (lift > 0f) {
                            translate(top = 5.dp.toPx() * lift) {
                                drawPath(mask, Color.Black.copy(alpha = 0.13f * lift))
                                contourPaths.forEach { contour ->
                                    drawPath(contour, Color.Black.copy(alpha = 0.04f * lift),
                                        style = Stroke(6.dp.toPx(), join = StrokeJoin.Round))
                                }
                            }
                        }
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

                        // A quiet resting border keeps the picture readable. No internal seams.
                        contourPaths.forEach { contour ->
                            drawPath(contour, if (isDragging) dragAccent else gridBorder.copy(alpha = 0.8f),
                                style = Stroke(if (isDragging) 2.dp.toPx() else 1.25.dp.toPx(),
                                    cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }

                        if (mergeProgress.value < 1f) {
                            val progress = mergeProgress.value
                            val alpha = smoothFraction(progress / 0.12f) *
                                (1f - smoothFraction((progress - 0.82f) / 0.18f))

                            // First trace the exact outer shape of the newly merged cluster.
                            // A soft halo sits behind the gold line so the boundary reads clearly
                            // without covering the artwork.
                            val traced = smoothFraction((progress - 0.06f) / 0.58f)
                            var remainingLength = perimeterLength * traced
                            contourMeasures.forEach { measure ->
                                val distance = remainingLength.coerceIn(0f, measure.length)
                                if (distance > 0f && alpha > 0f) {
                                    val trail = Path()
                                    measure.getSegment(0f, distance, trail, true)
                                    drawPath(
                                        trail,
                                        Color(0xFFFFD86B).copy(alpha = alpha * 0.18f),
                                        style = Stroke(10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                    drawPath(
                                        trail,
                                        Color(0xFFF6C554).copy(alpha = alpha),
                                        style = Stroke(2.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                    drawPath(
                                        trail,
                                        Color.White.copy(alpha = alpha * 0.94f),
                                        style = Stroke(0.9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                                remainingLength -= measure.length
                            }

                            // Once most of the border has been traced, make the whole contour
                            // breathe once. This gives the merge a clean "locked together" moment.
                            val lockPulse = smoothFraction((progress - 0.42f) / 0.16f) *
                                (1f - smoothFraction((progress - 0.78f) / 0.18f))
                            if (lockPulse > 0f) {
                                contourPaths.forEach { contour ->
                                    drawPath(
                                        contour,
                                        Color(0xFFFFD86B).copy(alpha = lockPulse * 0.18f),
                                        style = Stroke(12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                    drawPath(
                                        contour,
                                        Color(0xFFFFE99C).copy(alpha = lockPulse * 0.82f),
                                        style = Stroke(2.1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                            }

                            // Magic dust is emitted from points all around the real merged contour.
                            // The particle count increases with group size, so bigger merges feel
                            // more rewarding without making tiny 2-piece joins visually noisy.
                            val dustTimeline = ((progress - 0.10f) / 0.82f).coerceIn(0f, 1f)
                            if (dustTimeline > 0f && perimeterLength > 0f) {
                                val particleCount = (10 + group.size * 3).coerceIn(16, 58)
                                val center = Offset(size.width / 2f, size.height / 2f)
                                repeat(particleCount) { index ->
                                    val h1 = magicUnit(index * 97 + group.size * 31 + celebrationVersion * 17)
                                    val h2 = magicUnit(index * 193 + group.size * 53 + celebrationVersion * 29)
                                    val h3 = magicUnit(index * 389 + group.size * 71 + celebrationVersion * 37)
                                    val delayFraction = h2 * 0.24f
                                    val local = ((dustTimeline - delayFraction) /
                                        (1f - delayFraction)).coerceIn(0f, 1f)
                                    if (local > 0f) {
                                        val particleAlpha = smoothFraction(local / 0.16f) *
                                            (1f - smoothFraction((local - 0.48f) / 0.52f))
                                        if (particleAlpha > 0f) {
                                            val contourDistance = perimeterLength *
                                                ((index.toFloat() + 0.20f + h1 * 0.60f) / particleCount.toFloat())
                                                    .coerceIn(0f, 0.9999f)
                                            val edgePoint = pointAlongContours(contourMeasures, contourDistance)
                                            val dx = edgePoint.x - center.x
                                            val dy = edgePoint.y - center.y
                                            val length = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                                            val outwardX = dx / length
                                            val outwardY = dy / length
                                            val tangentX = -outwardY
                                            val tangentY = outwardX
                                            val travel = (4.dp.toPx() + h1 * 20.dp.toPx()) *
                                                smoothFraction(local)
                                            val sideDrift = (h2 - 0.5f) * 12.dp.toPx() * local
                                            val px = edgePoint.x + outwardX * travel + tangentX * sideDrift
                                            val py = edgePoint.y + outwardY * travel + tangentY * sideDrift
                                            val radius = (0.9.dp.toPx() + h3 * 1.8.dp.toPx())
                                            val gold = if (index % 4 == 0) {
                                                Color.White
                                            } else {
                                                Color(0xFFFFD968)
                                            }

                                            drawCircle(
                                                color = gold.copy(alpha = particleAlpha * 0.20f),
                                                radius = radius * 2.4f,
                                                center = Offset(px, py)
                                            )
                                            drawCircle(
                                                color = gold.copy(alpha = particleAlpha),
                                                radius = radius,
                                                center = Offset(px, py)
                                            )

                                            // A few particles become tiny four-point stars,
                                            // matching the magical sparkle feel in the reference.
                                            if (index % 3 == 0) {
                                                val arm = radius * (2.2f + h1)
                                                drawLine(
                                                    color = Color.White.copy(alpha = particleAlpha * 0.92f),
                                                    start = Offset(px - arm, py),
                                                    end = Offset(px + arm, py),
                                                    strokeWidth = 0.8.dp.toPx(),
                                                    cap = StrokeCap.Round
                                                )
                                                drawLine(
                                                    color = Color.White.copy(alpha = particleAlpha * 0.92f),
                                                    start = Offset(px, py - arm),
                                                    end = Offset(px, py + arm),
                                                    strokeWidth = 0.8.dp.toPx(),
                                                    cap = StrokeCap.Round
                                                )
                                            }
                                        }
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
                                    .pointerInput(groupKey, hitTileId, boardVersion, tileWidthPx, tileHeightPx, inputEnabled) {
                                        if (inputEnabled) detectDragGestures(
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

/** Smooth endpoints avoid a pop at lift-off and a snap when the block lands. */
private fun smoothFraction(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}


private fun pointAlongContours(measures: List<PathMeasure>, distance: Float): Offset {
    if (measures.isEmpty()) return Offset.Zero
    var remaining = distance.coerceAtLeast(0f)
    measures.forEach { measure ->
        if (remaining <= measure.length) {
            return measure.getPosition(remaining.coerceIn(0f, measure.length))
        }
        remaining -= measure.length
    }
    val last = measures.last()
    return last.getPosition(last.length.coerceAtLeast(0f))
}

/** Stable pseudo-random 0..1 value so particles never jitter between animation frames. */
private fun magicUnit(seed: Int): Float {
    val mixed = (seed.toLong() * 1_103_515_245L + 12_345L) and 0x7fffffffL
    return mixed.toFloat() / 0x7fffffffL.toFloat()
}

private fun mergeLift(progress: Float): Float =
    smoothFraction(progress / 0.20f) * (1f - smoothFraction((progress - 0.70f) / 0.30f))
