package com.hb.puzz.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    var timeExpired by remember(level.id, activeGridSize) { mutableStateOf(false) }
    var remainingSeconds by remember(level.id, activeGridSize) { mutableIntStateOf(initialChallengeSeconds) }
    var timerStarted by remember(level.id, activeGridSize) { mutableStateOf(false) }
    var timerRunning by remember(level.id, activeGridSize) { mutableStateOf(false) }
    var timerExpanded by remember(level.id, activeGridSize) { mutableStateOf(false) }
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
            stopwatchElapsedSeconds = 0
            clockMode = PuzzleClockMode.COUNTDOWN
            timerStarted = saved.timerStarted
            timerRunning = false
            timerExpanded = false
            isSolved = engine.isSolved()
            timeExpired = timerStarted && remainingSeconds <= 0 && !isSolved
        } else {
            remainingSeconds = initialChallengeSeconds
            stopwatchElapsedSeconds = 0
            clockMode = PuzzleClockMode.COUNTDOWN
            timerStarted = false
            timerRunning = false
            timerExpanded = false
            settings.saveSession(
                level.id,
                activeGridSize,
                engine.getCurrentPositions(),
                0,
                remainingSeconds,
                clockMode,
                stopwatchElapsedSeconds,
                timerStarted
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

    // The challenge clock only runs after the player explicitly presses Start or resumes it.
    LaunchedEffect(initialized, timerRunning, isSolved, imageLoading, puzzleImage, level.id, activeGridSize) {
        if (!initialized || !timerRunning || isSolved || imageLoading || puzzleImage == null || timeExpired) {
            return@LaunchedEffect
        }
        while (timerRunning && !isSolved && remainingSeconds > 0) {
            delay(1_000)
            if (timerRunning && !isSolved && remainingSeconds > 0) {
                remainingSeconds--
                if (remainingSeconds % 5 == 0) {
                    settings.saveSession(
                        level.id, activeGridSize, engine.getCurrentPositions(), moveCount,
                        remainingSeconds, PuzzleClockMode.COUNTDOWN, 0, timerStarted
                    )
                }
                if (remainingSeconds == 0) {
                    timeExpired = true
                    timerRunning = false
                    timerExpanded = false
                    settings.saveSession(
                        level.id, activeGridSize, engine.getCurrentPositions(), moveCount,
                        0, PuzzleClockMode.COUNTDOWN, 0, true
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

    fun persistTimerState() {
        scope.launch {
            settings.saveSession(
                level.id,
                activeGridSize,
                engine.getCurrentPositions(),
                moveCount,
                remainingSeconds,
                PuzzleClockMode.COUNTDOWN,
                0,
                timerStarted
            )
        }
    }

    fun adjustChallengeTime(deltaSeconds: Int) {
        if (timeExpired || isSolved) return
        val upperLimit = (initialChallengeSeconds + 10 * 60).coerceAtMost(30 * 60)
        remainingSeconds = (remainingSeconds + deltaSeconds).coerceIn(30, upperLimit)
        persistTimerState()
    }

    fun openOrResumeTimer() {
        if (!initialized || isSolved || timeExpired) return
        timerExpanded = true
        if (timerStarted) {
            timerRunning = true
            persistTimerState()
        }
    }

    fun startTimer() {
        if (!initialized || isSolved || timeExpired) return
        timerStarted = true
        timerRunning = true
        timerExpanded = true
        persistTimerState()
    }

    fun pauseAndCollapseTimer() {
        if (!timerRunning || isSolved) return
        timerRunning = false
        timerExpanded = false
        persistTimerState()
    }

    fun restart() {
        engine.shuffle()
        moveCount = 0
        remainingSeconds = initialChallengeSeconds
        stopwatchElapsedSeconds = 0
        clockMode = PuzzleClockMode.COUNTDOWN
        timerStarted = false
        timerRunning = false
        timerExpanded = false
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
                PuzzleClockMode.COUNTDOWN,
                0,
                false
            )
        }
    }

    // Warm cream backdrop: calm enough for long play sessions while keeping the artwork vivid.
    val gameBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFF7E8),
            Color(0xFFF9EEDB),
            Color(0xFFF3E6D2)
        )
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(gameBackground)
            .padding(horizontal = 2.dp)
    ) {
        /*
         * Constant-layout timer architecture:
         * The puzzle board NEVER moves when the timer opens or closes. We permanently
         * reserve the same header depth used by the expanded/running timer state, then
         * render the timer as a morphing overlay inside that space. This keeps every tile
         * at the exact same screen coordinate throughout timer interactions.
         */
        val timerPanelVisible = timerExpanded && !timeExpired
        val boardTopZone = 232.dp
        val bottomZone = 126.dp
        val boardAspect = 1.28f // portrait board without excessive vertical stretching
        val horizontalBoardSpace = (maxWidth - 8.dp).coerceAtLeast(220.dp)
        val verticalBoardSpace = (maxHeight - boardTopZone - bottomZone - 8.dp).coerceAtLeast(260.dp)
        val widthAllowedByHeight = verticalBoardSpace / boardAspect
        val boardWidth = minOf(horizontalBoardSpace, widthAllowedByHeight)
        val boardHeight = boardWidth * boardAspect
        val connectionProgress = if (totalConnections == 0) 0f
        else (connectionCount.toFloat() / totalConnections.toFloat()).coerceIn(0f, 1f)

        // TOP SAFE HUD: one stateful timer control + a non-layout timer morph stage.
        // The stage is intentionally a fixed overlay. Expanding/collapsing it never changes
        // boardTopZone, so the puzzle remains perfectly stationary.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(boardTopZone)
                .statusBarsPadding()
                .padding(top = 10.dp, start = 10.dp, end = 10.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier.align(Alignment.CenterStart),
                    shape = CircleShape,
                    color = Color(0xFFFFFBF3).copy(alpha = 0.96f)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF354A46)
                        )
                    }
                }

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Chapter ${level.id} · ${level.title}",
                        modifier = Modifier.widthIn(max = 205.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2E403D),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "$activeGridSize×$activeGridSize · ${level.difficulty.shortLabel} · Par $parMoves moves",
                        modifier = Modifier.widthIn(max = 220.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF746E63),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    shape = CircleShape,
                    color = if (timerRunning) Color(0xFF2F7779) else Color(0xFFFFFBF3).copy(alpha = 0.96f),
                    tonalElevation = 3.dp,
                    shadowElevation = 4.dp
                ) {
                    IconButton(
                        onClick = {
                            when {
                                timerRunning -> pauseAndCollapseTimer()
                                timerExpanded && !timerStarted -> timerExpanded = false
                                else -> openOrResumeTimer()
                            }
                        },
                        enabled = initialized && !isSolved && !timeExpired
                    ) {
                        Icon(
                            imageVector = if (timerRunning) Icons.Default.Pause else Icons.Default.Timer,
                            contentDescription = if (timerRunning) "Pause challenge timer" else "Open challenge timer",
                            tint = if (timerRunning) Color.White else Color(0xFF354A46)
                        )
                    }
                }
            }

            /*
             * "Magic bloat" timer morph:
             * - transformOrigin is near the top-right timer icon
             * - spring growth makes the card feel like it blooms from the icon
             * - reverse scale/fade visually folds it back into the same control
             * - because this is an overlay, it contributes zero layout displacement
             */
            AnimatedVisibility(
                visible = timerPanelVisible,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 66.dp),
                enter = fadeIn(animationSpec = tween(150)) +
                    scaleIn(
                        animationSpec = spring(
                            dampingRatio = 0.84f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        initialScale = 0.16f,
                        transformOrigin = TransformOrigin(0.94f, 0.02f)
                    ),
                exit = fadeOut(animationSpec = tween(210)) +
                    scaleOut(
                        animationSpec = tween(
                            durationMillis = 390,
                            easing = FastOutSlowInEasing
                        ),
                        targetScale = 0.16f,
                        transformOrigin = TransformOrigin(0.94f, 0.02f)
                    )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .border(
                            1.dp,
                            Color(0xFF78A7A1).copy(alpha = 0.55f),
                            RoundedCornerShape(26.dp)
                        ),
                    shape = RoundedCornerShape(26.dp),
                    color = if (remainingSeconds <= 60) Color(0xFFFFE4DA) else Color(0xFFFFFBF3),
                    tonalElevation = 3.dp,
                    shadowElevation = 9.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { adjustChallengeTime(-30) },
                                enabled = initialized && !isSolved && remainingSeconds > 30,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Text(
                                    "−30",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF354A46)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    formatChallengeTime(remainingSeconds),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    color = if (remainingSeconds <= 60) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        Color(0xFF2F6765)
                                    }
                                )
                                Text(
                                    if (timerRunning) "CHALLENGE CLOCK · RUNNING" else "CHALLENGE CLOCK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF5E655F)
                                )
                            }

                            IconButton(
                                onClick = { adjustChallengeTime(30) },
                                enabled = initialized && !isSolved,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Text(
                                    "+30",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF354A46)
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        if (!timerStarted) {
                            Button(
                                onClick = { startTimer() },
                                enabled = initialized && !isSolved,
                                modifier = Modifier.height(40.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Start")
                            }
                        } else {
                            Text(
                                "$moveCount moves · tap the top-right control to pause",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF77766E).copy(alpha = 0.88f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                .align(Alignment.TopCenter)
                .offset(y = boardTopZone)
                .width(boardWidth + 6.dp)
                .height(boardHeight + 6.dp),
            shape = RoundedCornerShape(13.dp),
            color = Color(0xFFFFF8EC),
            tonalElevation = 2.dp,
            shadowElevation = 10.dp
        ) {
            Box(
                modifier = Modifier.padding(2.dp),
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
                                            timerRunning = false
                                            timerExpanded = false
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
                                                    PuzzleClockMode.COUNTDOWN,
                                                    0,
                                                    timerStarted
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
                .padding(bottom = 10.dp, start = 8.dp, end = 8.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFFFFBF4).copy(alpha = 0.98f),
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
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF303B37)
                            )
                            Text(
                                "$connectionCount of $totalConnections connections",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6C6B63)
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
                                Color(0xFFE9E0D4),
                                CircleShape
                            )
                    ) {
                        if (connectionProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(connectionProgress)
                                    .height(7.dp)
                                    .background(Color(0xFF4F9DA0), CircleShape)
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

    if (timeExpired) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Time’s up ⏱") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("This countdown attempt has ended. Restart the puzzle to try again.")
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "00:00",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(onClick = { restart() }) { Text("Restart puzzle") }
            },
            dismissButton = {
                TextButton(onClick = onHome) { Text("Home") }
            }
        )
    }
}

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

