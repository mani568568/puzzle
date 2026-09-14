package com.hb.puzz.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hb.puzz.data.GameSettings
import com.hb.puzz.data.PuzzleClockMode
import com.hb.puzz.data.images.ImageSourceMode
import com.hb.puzz.data.images.PuzzleImage
import com.hb.puzz.data.images.PuzzleImageRepository
import com.hb.puzz.domain.PuzzleEngine
import com.hb.puzz.domain.PuzzleLevel
import com.hb.puzz.ui.feedback.PuzzleFeedbackPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun PicturePuzzleGameScreen(
    levelId: Int,
    gridSize: Int,
    settings: GameSettings,
    imageRepository: PuzzleImageRepository,
    imageSource: ImageSourceMode,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onNextLevel: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val level = PuzzleLevel.requireLevel(levelId)
    val activeGridSize = gridSize.takeIf(level::acceptsGridSize) ?: level.gridSize
    val engine = remember(level.id, activeGridSize) { PuzzleEngine(activeGridSize, level.seed) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val feedback = remember { PuzzleFeedbackPlayer(context) }
    val totalConnections = remember(level.id, activeGridSize) { engine.getTotalPossibleConnections() }
    val parMoves = remember(level.id, activeGridSize) { recommendedParMoves(activeGridSize, totalConnections) }
    val initialChallengeSeconds = remember(level.id, activeGridSize) {
        defaultChallengeSeconds(activeGridSize, totalConnections)
    }

    var moveCount by remember(level.id, activeGridSize) { mutableIntStateOf(0) }
    var boardVersion by remember(level.id, activeGridSize) { mutableIntStateOf(0) }
    var isSolved by remember(level.id, activeGridSize) { mutableStateOf(false) }
    var isPaused by remember(level.id, activeGridSize) { mutableStateOf(false) }
    var timeExpired by remember(level.id, activeGridSize) { mutableStateOf(false) }
    var remainingSeconds by remember(level.id, activeGridSize) { mutableIntStateOf(initialChallengeSeconds) }
    var stopwatchElapsedSeconds by remember(level.id, activeGridSize) { mutableIntStateOf(0) }
    var clockMode by remember(level.id, activeGridSize) { mutableStateOf(PuzzleClockMode.COUNTDOWN) }
    var initialized by remember(level.id, activeGridSize) { mutableStateOf(false) }
    var puzzleImage by remember(level.id, imageSource) { mutableStateOf<PuzzleImage?>(null) }
    var imageLoading by remember(level.id, imageSource) { mutableStateOf(true) }
    var refreshToken by remember(level.id, imageSource) { mutableIntStateOf(0) }

    var connectionCount by remember(level.id, activeGridSize) { mutableIntStateOf(0) }
    var celebratingTileIds by remember(level.id, activeGridSize) { mutableStateOf(emptySet<Int>()) }
    var celebrationMessage by remember(level.id, activeGridSize) { mutableStateOf<String?>(null) }
    var celebrationVersion by remember(level.id, activeGridSize) { mutableIntStateOf(0) }
    var completionDetailsVisible by remember(level.id, activeGridSize) { mutableStateOf(false) }

    fun refreshConnectionState() {
        val connections = engine.getCorrectConnections()
        connectionCount = connections.size
    }

    DisposableEffect(feedback) {
        onDispose { feedback.release() }
    }

    LaunchedEffect(level.id, activeGridSize) {
        val saved = settings.loadSession()
        if (saved?.levelId == level.id && saved.gridSize == activeGridSize && engine.restorePositions(saved.positions)) {
            moveCount = saved.moveCount
            remainingSeconds = saved.remainingSeconds ?: initialChallengeSeconds
            stopwatchElapsedSeconds = saved.stopwatchElapsedSeconds
            clockMode = saved.clockMode
            isSolved = engine.isSolved()
            timeExpired = clockMode == PuzzleClockMode.COUNTDOWN && remainingSeconds <= 0 && !isSolved
            isPaused = timeExpired
        } else {
            remainingSeconds = initialChallengeSeconds
            stopwatchElapsedSeconds = 0
            clockMode = PuzzleClockMode.COUNTDOWN
            settings.saveSession(
                level.id,
                activeGridSize,
                engine.getCurrentPositions(),
                0,
                remainingSeconds,
                clockMode,
                stopwatchElapsedSeconds
            )
        }
        refreshConnectionState()
        boardVersion++
        initialized = true
    }

    LaunchedEffect(level.id, imageSource, refreshToken) {
        imageLoading = true
        puzzleImage = if (imageSource == ImageSourceMode.PEXELS && refreshToken > 0) {
            imageRepository.refreshLevelImage(level)
        } else {
            imageRepository.loadLevelImage(level, imageSource)
        }
        imageLoading = false
    }

    // Countdown mode: reaching 00:00 ends the attempt. The only way to continue is Restart.
    LaunchedEffect(initialized, isPaused, isSolved, imageLoading, puzzleImage, level.id, activeGridSize, clockMode) {
        if (clockMode != PuzzleClockMode.COUNTDOWN) return@LaunchedEffect
        if (!initialized || isPaused || isSolved || imageLoading || puzzleImage == null) return@LaunchedEffect
        while (!isPaused && !isSolved && clockMode == PuzzleClockMode.COUNTDOWN && remainingSeconds > 0) {
            delay(1_000)
            if (!isPaused && !isSolved && clockMode == PuzzleClockMode.COUNTDOWN && remainingSeconds > 0) {
                remainingSeconds--
                if (remainingSeconds % 5 == 0) {
                    settings.saveSession(
                        level.id, activeGridSize, engine.getCurrentPositions(), moveCount,
                        remainingSeconds, clockMode, stopwatchElapsedSeconds
                    )
                }
                if (remainingSeconds == 0) {
                    timeExpired = true
                    isPaused = true
                    settings.saveSession(
                        level.id, activeGridSize, engine.getCurrentPositions(), moveCount,
                        0, clockMode, stopwatchElapsedSeconds
                    )
                }
            }
        }
    }

    // Stopwatch mode: counts upward with no timeout. Pause freezes it just like countdown mode.
    LaunchedEffect(initialized, isPaused, isSolved, imageLoading, puzzleImage, level.id, activeGridSize, clockMode) {
        if (clockMode != PuzzleClockMode.STOPWATCH) return@LaunchedEffect
        if (!initialized || isPaused || isSolved || imageLoading || puzzleImage == null) return@LaunchedEffect
        while (!isPaused && !isSolved && clockMode == PuzzleClockMode.STOPWATCH) {
            delay(1_000)
            if (!isPaused && !isSolved && clockMode == PuzzleClockMode.STOPWATCH) {
                stopwatchElapsedSeconds++
                if (stopwatchElapsedSeconds % 5 == 0) {
                    settings.saveSession(
                        level.id, activeGridSize, engine.getCurrentPositions(), moveCount,
                        remainingSeconds, clockMode, stopwatchElapsedSeconds
                    )
                }
            }
        }
    }

    // Let the player enjoy the fully restored artwork before showing results.
    LaunchedEffect(isSolved, puzzleImage) {
        completionDetailsVisible = false
        if (isSolved && puzzleImage != null) {
            delay(2_000)
            completionDetailsVisible = true
        }
    }

    fun showConnectionCelebration(newTileIds: Set<Int>, newConnectionCount: Int) {
        val mergedGroups = engine.getConnectedGroups()
            .filter { group -> group.any { it in newTileIds } }
        celebratingTileIds = mergedGroups.flatten().toSet().ifEmpty { newTileIds }
        val linkedGroupSize = mergedGroups
            .maxOfOrNull { it.size }
            ?: newTileIds.size

        celebrationMessage = when {
            linkedGroupSize >= 5 -> "Amazing! $linkedGroupSize pieces linked ✨"
            linkedGroupSize >= 3 -> "Great combo! $linkedGroupSize pieces linked ✨"
            newConnectionCount > 1 -> "Nice! $newConnectionCount new connections ✨"
            else -> "Perfect fit! ✨"
        }

        celebrationVersion++
        val token = celebrationVersion
        scope.launch {
            delay(900)
            if (celebrationVersion == token) {
                celebratingTileIds = emptySet()
                celebrationMessage = null
            }
        }
    }

    fun adjustChallengeTime(deltaSeconds: Int) {
        if (clockMode != PuzzleClockMode.COUNTDOWN || timeExpired) return
        val upperLimit = (initialChallengeSeconds + 10 * 60).coerceAtMost(30 * 60)
        remainingSeconds = (remainingSeconds + deltaSeconds).coerceIn(30, upperLimit)
        scope.launch {
            settings.saveSession(
                level.id, activeGridSize, engine.getCurrentPositions(), moveCount,
                remainingSeconds, clockMode, stopwatchElapsedSeconds
            )
        }
    }

    fun switchClockMode() {
        if (!initialized || isSolved || timeExpired) return
        clockMode = if (clockMode == PuzzleClockMode.COUNTDOWN) {
            PuzzleClockMode.STOPWATCH
        } else {
            PuzzleClockMode.COUNTDOWN
        }
        scope.launch {
            settings.saveSession(
                level.id, activeGridSize, engine.getCurrentPositions(), moveCount,
                remainingSeconds, clockMode, stopwatchElapsedSeconds
            )
        }
    }

    fun restart() {
        engine.shuffle()
        moveCount = 0
        remainingSeconds = initialChallengeSeconds
        stopwatchElapsedSeconds = 0
        timeExpired = false
        isSolved = false
        celebratingTileIds = emptySet()
        celebrationMessage = null
        completionDetailsVisible = false
        refreshConnectionState()
        boardVersion++
        scope.launch {
            settings.saveSession(
                level.id,
                activeGridSize,
                engine.getCurrentPositions(),
                0,
                remainingSeconds,
                clockMode,
                stopwatchElapsedSeconds
            )
        }
    }

    val gameBackground = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.82f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f)
        )
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(gameBackground)
            .padding(horizontal = 2.dp)
    ) {
        // Keep the controls compact and let the board use the phone as a portrait rectangle.
        val topZone = 150.dp
        val bottomZone = 104.dp
        val horizontalBoardSpace = (maxWidth - 4.dp).coerceAtLeast(220.dp)
        val verticalBoardSpace = (maxHeight - topZone - bottomZone).coerceAtLeast(300.dp)
        val boardWidth = horizontalBoardSpace
        val boardHeight = minOf(verticalBoardSpace, boardWidth * 1.28f)
        val connectionProgress = if (totalConnections == 0) 0f
        else (connectionCount.toFloat() / totalConnections.toFloat()).coerceIn(0f, 1f)

        // Soft ambient layers keep attention around the puzzle without competing with the photo.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(190.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(220.dp)
                .background(
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f),
                    CircleShape
                )
        )

        // TOP ~1 INCH: compact journey HUD.
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp, start = 6.dp, end = 6.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Chapter ${level.id} · ${level.title}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$activeGridSize×$activeGridSize · ${level.difficulty.shortLabel} · Par $parMoves moves",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { isPaused = true }, enabled = initialized && !isSolved) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .pointerInput(clockMode, initialized, isSolved, timeExpired) {
                                var swipeDistance = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { swipeDistance = 0f },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        swipeDistance += dragAmount
                                    },
                                    onDragEnd = {
                                        if (abs(swipeDistance) > 70f) switchClockMode()
                                        swipeDistance = 0f
                                    },
                                    onDragCancel = { swipeDistance = 0f }
                                )
                            },
                        shape = RoundedCornerShape(28.dp),
                        color = if (clockMode == PuzzleClockMode.COUNTDOWN && remainingSeconds <= 60) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.96f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
                        },
                        tonalElevation = 3.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (clockMode == PuzzleClockMode.COUNTDOWN) {
                                    IconButton(
                                        onClick = { adjustChallengeTime(-30) },
                                        enabled = initialized && !isSolved && !timeExpired && remainingSeconds > 30,
                                        modifier = Modifier.size(36.dp)
                                    ) { Text("−30", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                                } else {
                                    Text("←", modifier = Modifier.padding(horizontal = 14.dp), fontWeight = FontWeight.Bold)
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val shownSeconds = if (clockMode == PuzzleClockMode.COUNTDOWN) remainingSeconds else stopwatchElapsedSeconds
                                    Text(
                                        formatChallengeTime(shownSeconds),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (clockMode == PuzzleClockMode.COUNTDOWN && remainingSeconds <= 60) {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        } else {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        }
                                    )
                                    Text(
                                        "${if (clockMode == PuzzleClockMode.COUNTDOWN) "COUNTDOWN" else "STOPWATCH"} · $moveCount moves",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (clockMode == PuzzleClockMode.COUNTDOWN) {
                                    IconButton(
                                        onClick = { adjustChallengeTime(30) },
                                        enabled = initialized && !isSolved && !timeExpired,
                                        modifier = Modifier.size(36.dp)
                                    ) { Text("+30", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                                } else {
                                    Text("→", modifier = Modifier.padding(horizontal = 14.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                "Swipe ↔ to switch ${if (clockMode == PuzzleClockMode.COUNTDOWN) "to stopwatch" else "to countdown"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                            )
                        }
                    }
                }
            }
        }

        // CENTER: portrait rectangular puzzle board. Logical grids remain 4×4–8×8, but cells
        // are rectangular so the artwork uses more of a phone's vertical canvas.
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 30.dp)
                .width(boardWidth + 6.dp)
                .height(boardHeight + 6.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.36f),
            tonalElevation = 4.dp,
            shadowElevation = 12.dp
        ) {
            Box(
                modifier = Modifier.padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    !initialized || imageLoading || puzzleImage == null -> Box(
                        Modifier.width(boardWidth).height(boardHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            if (imageSource == ImageSourceMode.PEXELS) {
                                Spacer(Modifier.height(12.dp))
                                Text("Preparing your puzzle…", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    !isSolved -> {
                        val loaded = puzzleImage!!
                        Box(
                            modifier = Modifier.width(boardWidth).height(boardHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            PuzzleBoard(
                                engine = engine,
                                boardVersion = boardVersion,
                                image = loaded.bitmap.asImageBitmap(),
                                celebratingTileIds = celebratingTileIds,
                                celebrationVersion = celebrationVersion,
                                onGroupDropped = { anchorTileId, targetPosition ->
                                    val beforeConnections = engine.getCorrectConnections()

                                    if (engine.attemptMoveGroup(anchorTileId, targetPosition)) {
                                        moveCount++
                                        val afterConnections = engine.getCorrectConnections()
                                        val newlyCreated = afterConnections - beforeConnections
                                        val gainedProgress = afterConnections.size > beforeConnections.size

                                        connectionCount = afterConnections.size

                                        val solvedNow = engine.isSolved()
                                        isSolved = solvedNow
                                        boardVersion++

                                        if (solvedNow) {
                                            celebratingTileIds = (0 until engine.getTotalTiles()).toSet()
                                            celebrationVersion++
                                            if (soundEnabled) feedback.playSolvedSound()
                                            if (hapticsEnabled) feedback.buzzForSolved()
                                        } else if (gainedProgress && newlyCreated.isNotEmpty()) {
                                            val newTileIds = newlyCreated
                                                .flatMap { listOf(it.firstTileId, it.secondTileId) }
                                                .toSet()
                                            showConnectionCelebration(newTileIds, newlyCreated.size)
                                            if (soundEnabled) feedback.playConnectionSound()
                                            if (hapticsEnabled) feedback.buzzForConnection()
                                        }

                                        scope.launch {
                                            if (solvedNow) {
                                                settings.markLevelCompleted(level.id)
                                                settings.clearSession()
                                            } else {
                                                settings.saveSession(
                                                    level.id,
                                                    activeGridSize,
                                                    engine.getCurrentPositions(),
                                                    moveCount,
                                                    remainingSeconds,
                                                    clockMode,
                                                    stopwatchElapsedSeconds
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            CelebrationBanner(
                                message = celebrationMessage,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 12.dp)
                            )
                        }
                    }

                    else -> {
                        val loaded = puzzleImage!!
                        val score = calculatePuzzleScore(
                            gridSize = activeGridSize,
                            totalConnections = totalConnections,
                            moves = moveCount,
                            remainingSeconds = remainingSeconds,
                            clockMode = clockMode,
                            stopwatchElapsedSeconds = stopwatchElapsedSeconds,
                            targetSeconds = initialChallengeSeconds
                        )
                        Box(modifier = Modifier.width(boardWidth).height(boardHeight)) {
                            Image(
                                bitmap = loaded.bitmap.asImageBitmap(),
                                contentDescription = "Completed chapter artwork",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            CompletionResultOverlay(
                                visible = completionDetailsVisible,
                                score = score,
                                moves = moveCount,
                                remainingSeconds = remainingSeconds,
                                clockMode = clockMode,
                                stopwatchElapsedSeconds = stopwatchElapsedSeconds,
                                chapterNumber = level.id,
                                nextChapterTitle = PuzzleLevel.getLevel(level.id + 1)?.title,
                                nextChapterDifficulty = PuzzleLevel.getLevel(level.id + 1)?.difficulty?.displayLabel,
                                nextChapterGrid = PuzzleLevel.getLevel(level.id + 1)?.gridDescription,
                                isLastChapter = level.id >= PuzzleLevel.maxLevelId,
                                onNext = { onNextLevel(level.id + 1) },
                                onFinish = onHome,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }

        // BOTTOM ~1 INCH: progress + low-priority controls. Kept visually quiet so the image wins.
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 5.dp, start = 3.dp, end = 3.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isSolved) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Picture progress",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "$connectionCount of $totalConnections connections",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { restart() },
                            enabled = initialized && !imageLoading
                        ) {
                            Text("Reshuffle")
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                CircleShape
                            )
                    ) {
                        if (connectionProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(connectionProgress)
                                    .height(7.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    }
                } else if (!completionDetailsVisible) {
                    Text(
                        "Take it in ✨",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Your completed picture is on display",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Chapter restored ✨",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                puzzleImage?.let { loaded ->
                    loaded.attribution?.let { credit ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                runCatching { uriHandler.openUri(credit.photoUrl) }
                            }) {
                                Text(
                                    "Photo by ${credit.photographer} · Pexels",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            if (!isSolved) {
                                IconButton(onClick = { refreshToken++ }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Load another photo")
                                }
                            }
                        }
                    }

                    if (loaded.usedFallback && !loaded.message.isNullOrBlank()) {
                        Text(
                            text = loaded.message,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (isPaused) {
        AlertDialog(
            onDismissRequest = { if (!timeExpired) isPaused = false },
            title = { Text(if (timeExpired) "Time’s up ⏱" else "Puzzle paused") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (timeExpired) {
                        Text("This countdown attempt has ended. Restart the puzzle to try again.")
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "00:00",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text("${if (clockMode == PuzzleClockMode.COUNTDOWN) "Countdown" else "Stopwatch"} is frozen while paused.")
                        Spacer(Modifier.height(14.dp))
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                formatChallengeTime(
                                    if (clockMode == PuzzleClockMode.COUNTDOWN) remainingSeconds else stopwatchElapsedSeconds
                                ),
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (timeExpired) {
                    Button(onClick = {
                        restart()
                        isPaused = false
                    }) { Text("Restart puzzle") }
                } else {
                    Button(onClick = { isPaused = false }) { Text("Resume") }
                }
            },
            dismissButton = {
                Row {
                    if (!timeExpired) {
                        TextButton(onClick = {
                            restart()
                            isPaused = false
                        }) { Text("Restart") }
                    }
                    TextButton(onClick = onHome) { Text("Home") }
                }
            }
        )
    }}

private fun recommendedParMoves(gridSize: Int, totalConnections: Int): Int =
    gridSize * gridSize + totalConnections / 2

private fun defaultChallengeSeconds(gridSize: Int, totalConnections: Int): Int {
    val parMoves = recommendedParMoves(gridSize, totalConnections)
    return (parMoves * 6).coerceAtLeast(3 * 60)
}

private fun formatChallengeTime(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val minutes = safe / 60
    val seconds = safe % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun calculatePuzzleScore(
    gridSize: Int,
    totalConnections: Int,
    moves: Int,
    remainingSeconds: Int,
    clockMode: PuzzleClockMode,
    stopwatchElapsedSeconds: Int,
    targetSeconds: Int
): Int {
    val baseScore = gridSize * gridSize * 250 + totalConnections * 100
    val movePenalty = moves * 15
    val timeBonus = when (clockMode) {
        PuzzleClockMode.COUNTDOWN -> remainingSeconds.coerceAtLeast(0) * 5
        PuzzleClockMode.STOPWATCH -> (targetSeconds - stopwatchElapsedSeconds).coerceAtLeast(0) * 3
    }
    return (baseScore + timeBonus - movePenalty).coerceAtLeast(gridSize * gridSize * 50)
}

@Composable
private fun CompletionResultOverlay(
    visible: Boolean,
    score: Int,
    moves: Int,
    remainingSeconds: Int,
    clockMode: PuzzleClockMode,
    stopwatchElapsedSeconds: Int,
    chapterNumber: Int,
    nextChapterTitle: String?,
    nextChapterDifficulty: String?,
    nextChapterGrid: String?,
    isLastChapter: Boolean,
    onNext: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(260)) + scaleIn(initialScale = 0.90f),
        exit = fadeOut()
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 10.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 30.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "✨ Chapter $chapterNumber Complete!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = score.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("JOURNEY SCORE", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                Text("Solved in $moves moves", style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (clockMode == PuzzleClockMode.COUNTDOWN) {
                        "${formatChallengeTime(remainingSeconds)} left on the countdown"
                    } else {
                        "Completed in ${formatChallengeTime(stopwatchElapsedSeconds)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(18.dp))
                if (!isLastChapter) {
                    nextChapterTitle?.let {
                        Text(
                            text = "Next: Chapter ${chapterNumber + 1} · $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (nextChapterGrid != null && nextChapterDifficulty != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "$nextChapterGrid  •  $nextChapterDifficulty",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    Button(onClick = onNext) { Text("Continue to Chapter ${chapterNumber + 1}") }
                } else {
                    Text(
                        "You restored every picture in the journey ✨",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onFinish) { Text("Finish Journey") }
                }
            }
        }
    }
}

@Composable
private fun CelebrationBanner(
    message: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn() + scaleIn(initialScale = 0.82f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.96f),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Text(
                text = message.orEmpty(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

