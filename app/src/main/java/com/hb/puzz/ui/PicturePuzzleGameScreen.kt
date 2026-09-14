package com.hb.puzz.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hb.puzz.data.GameSettings
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
    val engine = remember(level.id) { PuzzleEngine(level.gridSize, level.seed) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val feedback = remember { PuzzleFeedbackPlayer(context) }
    val totalConnections = remember(level.id) { engine.getTotalPossibleConnections() }

    var moveCount by remember(level.id) { mutableIntStateOf(0) }
    var boardVersion by remember(level.id) { mutableIntStateOf(0) }
    var isSolved by remember(level.id) { mutableStateOf(false) }
    var isPaused by remember(level.id) { mutableStateOf(false) }
    var initialized by remember(level.id) { mutableStateOf(false) }
    var puzzleImage by remember(level.id, imageSource) { mutableStateOf<PuzzleImage?>(null) }
    var imageLoading by remember(level.id, imageSource) { mutableStateOf(true) }
    var refreshToken by remember(level.id, imageSource) { mutableIntStateOf(0) }

    var connectionCount by remember(level.id) { mutableIntStateOf(0) }
    var celebratingTileIds by remember(level.id) { mutableStateOf(emptySet<Int>()) }
    var celebrationMessage by remember(level.id) { mutableStateOf<String?>(null) }
    var celebrationVersion by remember(level.id) { mutableIntStateOf(0) }
    var completionDetailsVisible by remember(level.id) { mutableStateOf(false) }

    fun refreshConnectionState() {
        val connections = engine.getCorrectConnections()
        connectionCount = connections.size
    }

    DisposableEffect(feedback) {
        onDispose { feedback.release() }
    }

    LaunchedEffect(level.id) {
        val saved = settings.loadSession()
        if (saved?.levelId == level.id && engine.restorePositions(saved.positions)) {
            moveCount = saved.moveCount
            isSolved = engine.isSolved()
        } else {
            settings.saveSession(level.id, engine.getCurrentPositions(), 0)
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

    fun restart() {
        engine.shuffle()
        moveCount = 0
        isSolved = false
        celebratingTileIds = emptySet()
        celebrationMessage = null
        completionDetailsVisible = false
        refreshConnectionState()
        boardVersion++
        scope.launch { settings.saveSession(level.id, engine.getCurrentPositions(), 0) }
    }

    val gameBackground = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.82f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f)
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gameBackground)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Level ${level.id}: ${level.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${level.gridSize} × ${level.gridSize} · $moveCount moves",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = { isPaused = true }, enabled = initialized && !isSolved) {
                Icon(Icons.Default.Pause, contentDescription = "Pause")
            }
        }

        if (!isSolved) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
                ) {
                    Text(
                        text = "Connections $connectionCount / $totalConnections",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        when {
            !initialized || imageLoading || puzzleImage == null -> Box(
                Modifier.fillMaxWidth().aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    if (imageSource == ImageSourceMode.PEXELS) {
                        Spacer(Modifier.height(12.dp))
                        Text("Loading puzzle photo…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            !isSolved -> {
                val loaded = puzzleImage!!
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
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
                                        settings.saveSession(level.id, engine.getCurrentPositions(), moveCount)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 2.dp)
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
                    gridSize = level.gridSize,
                    totalConnections = totalConnections,
                    moves = moveCount
                )
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                    // For the first two seconds after the final snap, this is intentionally the
                    // only content over the puzzle area: the player gets a clean image reveal.
                    Image(
                        bitmap = loaded.bitmap.asImageBitmap(),
                        contentDescription = "Completed level artwork",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    CompletionResultOverlay(
                        visible = completionDetailsVisible,
                        score = score,
                        moves = moveCount,
                        isLastLevel = level.id >= PuzzleLevel.maxLevelId,
                        onNext = { onNextLevel(level.id + 1) },
                        onFinish = onHome,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        puzzleImage?.let { loaded ->
            loaded.attribution?.let { credit ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = {
                        runCatching { uriHandler.openUri(credit.photoUrl) }
                    }) {
                        Text("Photo by ${credit.photographer} on Pexels")
                    }
                    if (!isSolved) {
                        IconButton(onClick = { refreshToken++ }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Load a different Pexels photo")
                        }
                    }
                }
            }

            if (loaded.usedFallback && !loaded.message.isNullOrBlank()) {
                Text(
                    text = loaded.message,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!isSolved) {
            OutlinedButton(
                onClick = { restart() },
                enabled = initialized && !imageLoading,
                modifier = Modifier.fillMaxWidth(0.72f).padding(top = 6.dp, bottom = 8.dp)
            ) {
                Text("Shuffle & Restart")
            }
        }
    }

    if (isPaused) {
        AlertDialog(
            onDismissRequest = { isPaused = false },
            title = { Text("Paused") },
            text = { Text("Resume, reshuffle this puzzle, or return home.") },
            confirmButton = { Button(onClick = { isPaused = false }) { Text("Resume") } },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        restart()
                        isPaused = false
                    }) { Text("Restart") }
                    TextButton(onClick = onHome) { Text("Home") }
                }
            }
        )
    }
}
private fun calculatePuzzleScore(
    gridSize: Int,
    totalConnections: Int,
    moves: Int
): Int {
    val baseScore = gridSize * gridSize * 250 + totalConnections * 100
    val movePenalty = moves * 15
    return (baseScore - movePenalty).coerceAtLeast(gridSize * gridSize * 50)
}

@Composable
private fun CompletionResultOverlay(
    visible: Boolean,
    score: Int,
    moves: Int,
    isLastLevel: Boolean,
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
                    "✨ Beautiful!",
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
                Text("SCORE", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                Text("Solved in $moves moves", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(18.dp))
                if (!isLastLevel) {
                    Button(onClick = onNext) { Text("Next Level") }
                } else {
                    Button(onClick = onFinish) { Text("Finish") }
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

