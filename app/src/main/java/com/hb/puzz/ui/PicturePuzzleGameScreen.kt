package com.hb.puzz.ui

import android.media.AudioManager
import android.media.ToneGenerator
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hb.puzz.data.GameSettings
import com.hb.puzz.data.images.ImageSourceMode
import com.hb.puzz.data.images.PuzzleImage
import com.hb.puzz.data.images.PuzzleImageRepository
import com.hb.puzz.domain.PuzzleEngine
import com.hb.puzz.domain.PuzzleLevel
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
    val haptics = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 35) }

    var moveCount by remember(level.id) { mutableIntStateOf(0) }
    var boardVersion by remember(level.id) { mutableIntStateOf(0) }
    var isSolved by remember(level.id) { mutableStateOf(false) }
    var isPaused by remember(level.id) { mutableStateOf(false) }
    var initialized by remember(level.id) { mutableStateOf(false) }
    var puzzleImage by remember(level.id, imageSource) { mutableStateOf<PuzzleImage?>(null) }
    var imageLoading by remember(level.id, imageSource) { mutableStateOf(true) }
    var refreshToken by remember(level.id, imageSource) { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    LaunchedEffect(level.id) {
        val saved = settings.loadSession()
        if (saved?.levelId == level.id && engine.restorePositions(saved.positions)) {
            moveCount = saved.moveCount
            isSolved = engine.isSolved()
        } else {
            settings.saveSession(level.id, engine.getCurrentPositions(), 0)
        }
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

    fun restart() {
        engine.shuffle()
        moveCount = 0
        isSolved = false
        boardVersion++
        scope.launch { settings.saveSession(level.id, engine.getCurrentPositions(), 0) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Level ${level.id}: ${level.title}", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${level.gridSize} × ${level.gridSize} · Moves: $moveCount",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = { isPaused = true }, enabled = initialized && !isSolved) {
                Icon(Icons.Default.Pause, contentDescription = "Pause")
            }
        }

        Spacer(Modifier.height(10.dp))

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
                Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                    PuzzleBoard(
                        engine = engine,
                        boardVersion = boardVersion,
                        image = loaded.bitmap.asImageBitmap(),
                        onTileDropped = { posA, posB ->
                            if (engine.attemptSwap(posA, posB)) {
                                moveCount++
                                boardVersion++
                                if (soundEnabled) toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 45)
                                if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                                val solvedNow = engine.isSolved()
                                isSolved = solvedNow
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
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            else -> {
                val loaded = puzzleImage!!
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                    Image(
                        bitmap = loaded.bitmap.asImageBitmap(),
                        contentDescription = "Completed level artwork",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 28.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Puzzle Solved!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Completed in $moveCount moves")
                        Spacer(Modifier.height(16.dp))
                        if (level.id < PuzzleLevel.maxLevelId) {
                            Button(onClick = { onNextLevel(level.id + 1) }) { Text("Next Level") }
                        } else {
                            Button(onClick = onHome) { Text("Finish") }
                        }
                    }
                }
            }
        }

        puzzleImage?.let { loaded ->
            loaded.attribution?.let { credit ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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

        OutlinedButton(
            onClick = { restart() },
            enabled = initialized && !imageLoading,
            modifier = Modifier.fillMaxWidth(0.8f).padding(vertical = 10.dp)
        ) { Text("Restart Level") }
    }

    if (isPaused) {
        AlertDialog(
            onDismissRequest = { isPaused = false },
            title = { Text("Paused") },
            text = { Text("Resume, restart this puzzle, or return home.") },
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
