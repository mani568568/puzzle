package com.hb.puzz.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hb.puzz.data.BlockMotionSpeed
import com.hb.puzz.data.GameSettings
import com.hb.puzz.data.images.ImageSourceMode
import com.hb.puzz.data.images.PuzzleImageRepository
import com.hb.puzz.domain.CoinRewards
import com.hb.puzz.domain.PuzzleLevel
import com.hb.puzz.ui.feedback.PuzzleFeedbackPlayer

@Composable
fun PicturePuzzleGameScreen(
    levelId: Int, gridSize: Int, settings: GameSettings,
    imageRepository: PuzzleImageRepository, imageSource: ImageSourceMode,
    soundEnabled: Boolean, hapticsEnabled: Boolean,
    onBack: () -> Unit, onHome: () -> Unit, onNextLevel: (Int) -> Unit,
    replayMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val factory = remember(levelId, gridSize, replayMode) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PuzzleGameViewModel(levelId, gridSize, settings, imageRepository, imageSource, replayMode) as T
        }
    }
    val vm: PuzzleGameViewModel = viewModel(key = "puzzle-$levelId-$gridSize-replay-$replayMode", factory = factory)
    val ui by vm.state.collectAsStateWithLifecycle()
    val wallet by settings.coinBalanceFlow.collectAsStateWithLifecycle(initialValue = 0)
    val crystals by settings.crystalBalanceFlow.collectAsStateWithLifecycle(initialValue = GameSettings.INITIAL_CRYSTALS)
    val hints by settings.hintBalanceFlow.collectAsStateWithLifecycle(initialValue = GameSettings.INITIAL_HINTS)
    val blockMotion by settings.blockMotionSpeedFlow.collectAsStateWithLifecycle(initialValue = BlockMotionSpeed.BALANCED)
    var confirmRestart by remember { mutableStateOf(false) }
    var rewardInfo by remember { mutableStateOf(false) }
    var crystalInfo by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val feedback = remember { PuzzleFeedbackPlayer(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentGridSize = ui.gridSize.takeIf { it >= 2 } ?: gridSize
    val baseGridSize = ui.baseGridSize.takeIf { it >= 2 } ?: gridSize
    val gridShifted = currentGridSize != baseGridSize
    val canDecreaseGrid = baseGridSize > PuzzleLevel.MIN_GRID_SIZE
    val totalConnections = vm.engine.getTotalPossibleConnections()
    val progress = ui.connections.toFloat() / totalConnections
    val animatedProgress by animateFloatAsState(progress, tween(400), label = "connections")
    val finishedTarget = ui.solved && ui.image != null
    val finishedReveal by animateFloatAsState(
        targetValue = if (finishedTarget) 1f else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "finished-reveal"
    )
    val finishedReady = finishedTarget && finishedReveal >= 0.92f
    val celebrationBurst = remember(levelId) { Animatable(0f) }
    val frameMergeGlow = remember(levelId) { Animatable(0f) }
    LaunchedEffect(ui.celebrationVersion) {
        if (ui.celebration.isNotEmpty()) {
            frameMergeGlow.snapTo(0.18f)
            frameMergeGlow.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
            frameMergeGlow.animateTo(0f, tween(820, easing = LinearOutSlowInEasing))
        }
    }
    LaunchedEffect(finishedTarget) {
        if (finishedTarget) {
            celebrationBurst.snapTo(0f)
            delay(140)
            celebrationBurst.animateTo(
                targetValue = 1f,
                animationSpec = tween(3950, easing = LinearOutSlowInEasing)
            )
        } else {
            celebrationBurst.snapTo(0f)
        }
    }
    val uri = LocalUriHandler.current

    DisposableEffect(lifecycle, vm) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> vm.pauseForLifecycle()
                Lifecycle.Event.ON_RESUME -> vm.resumeFromLifecycle()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            vm.pauseForLifecycle()
        }
    }
    DisposableEffect(feedback) { onDispose { feedback.release() } }
    BackHandler { vm.saveAndLeave(onBack) }

    // Calm game-play backdrop: cool sky at the top, soft aqua through the
    // puzzle area, and a warm ivory base near the controls.  Keeping the
    // saturation low prevents the background from competing with the artwork.
    val creamTop = Color(0xFFF5FBFF)
    val creamMid = Color(0xFFEEF8F7)
    val creamBottom = Color(0xFFFFF7EA)
    val softPanel = Color(0xFFEAFBFA)
    val glassAqua = Color(0xFFD8F5F3)
    val glassAquaDeep = Color(0xFFBFEAE8)
    val glassTealDark = Color(0xFF0A4E52)

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        creamTop,
                        creamMid,
                        creamBottom
                    )
                )
            )
            .safeDrawingPadding()
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VibrantCircleAction(
                    onClick = { vm.saveAndLeave(onBack) },
                    modifier = Modifier.size(48.dp),
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF35C6D0), Color(0xFF1F9DA6), Color(0xFF5F74F3))
                    ),
                    motion = ActionMotion.SHRINK
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Save and go back", tint = Color.White)
                }
                StandaloneTimer(
                    elapsedMillis = ui.elapsedMillis,
                    modifier = Modifier.widthIn(min = 148.dp, max = 172.dp)
                )
                ResourceNotificationAction(
                    value = crystals,
                    description = "$crystals crystals. Crystal Power",
                    badgeColor = Color(0xFF3FCBEA),
                    motion = ActionMotion.PULSE,
                    onClick = { crystalInfo = true },
                    icon = { CrystalIcon(29.dp) }
                )
                ResourceNotificationAction(
                    value = wallet.coerceAtMost(999999),
                    description = "$wallet gold coins. Reward details",
                    badgeColor = Color(0xFFFFB52E),
                    motion = ActionMotion.BOUNCE_UP,
                    onClick = { rewardInfo = true },
                    icon = { GoldCoinIcon(Modifier.size(29.dp)) }
                )
            }
            // The puzzle itself is now a true rectangular grid: fewer columns and more rows.
            // Match the board footprint to that logical geometry so cells remain balanced and
            // the puzzle uses substantially more vertical screen space down toward the controls.
            val puzzleAspectRatio = (vm.engine.gridColumns.toFloat() / vm.engine.gridRows.toFloat())
                .coerceIn(0.54f, 0.64f)
            // Keep the live puzzle framed while playing, but remove that outer board frame
            // completely during the magical finished transformation so no extra oval/outline
            // remains behind the final finished card.
            val puzzleFrameShape = RoundedCornerShape(9.dp)
            val puzzleContentShape = RoundedCornerShape(7.dp)
            val puzzleFrameColor = Color(0xFFFFF8E9)
            val puzzleFrameBorder = Color(0xFFD6AD43)
            val puzzleFrameGlow = Color(0xFFFFD86B)
            val boardContent: @Composable BoxScope.() -> Unit = {
                when {
                    ui.loading -> CircularProgressIndicator()
                    ui.error != null -> Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(ui.error!!)
                        VibrantAction(
                            onClick = vm::load,
                            modifier = Modifier.height(48.dp).fillMaxWidth(0.55f),
                            brush = VibrantOrangeBrush,
                            motion = ActionMotion.BOUNCE_UP
                        ) { Text("Try again", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                    ui.image != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = if (ui.solved) (1f - finishedReveal).coerceIn(0f, 1f) else 1f
                                    val settle = if (ui.solved) finishedReveal else 0f
                                    scaleX = 1f - (0.018f * settle)
                                    scaleY = 1f - (0.018f * settle)
                                }
                        ) {
                            PuzzleBoard(
                                vm.engine,
                                ui.boardVersion,
                                ui.image!!.bitmap.asImageBitmap(),
                                if (ui.solved) emptySet() else ui.celebration,
                                ui.celebrationVersion,
                                onGroupDropped = { anchor, target ->
                                    when (vm.drop(anchor, target)) {
                                        1 -> { if (soundEnabled) feedback.playConnectionSound(); if (hapticsEnabled) feedback.buzzForConnection() }
                                        2 -> { if (soundEnabled) feedback.playSolvedSound(); if (hapticsEnabled) feedback.buzzForSolved() }
                                    }
                                },
                                motionDurationMs = blockMotion.durationMillis,
                                modifier = Modifier.fillMaxSize(),
                                inputEnabled = !ui.solved && (!ui.started || ui.running)
                            )
                            if (!ui.solved) {
                                MergePraiseOverlay(
                                    mergeSize = ui.celebration.size,
                                    celebrationVersion = ui.celebrationVersion,
                                    solved = false,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        if (ui.solved) {
                            FinishedImageTransformation(
                                image = ui.image!!.bitmap.asImageBitmap(),
                                progress = finishedReveal,
                                celebrationProgress = celebrationBurst.value,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            if (ui.solved) {
                Box(
                    Modifier
                        .fillMaxWidth(0.80f)
                        .aspectRatio(puzzleAspectRatio),
                    contentAlignment = Alignment.Center,
                    content = boardContent
                )
            } else {
                // Layered champagne-gold frame. The low-alpha outer rings create a soft glow
                // without a black Material shadow. Each successful merge briefly strengthens the
                // halo so merges that touch the board edge still visibly light up the frame.
                val framePulse = frameMergeGlow.value.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.945f)
                        .aspectRatio(puzzleAspectRatio),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(12.dp),
                        color = puzzleFrameGlow.copy(alpha = 0.055f + 0.10f * framePulse),
                        border = BorderStroke(
                            4.dp,
                            puzzleFrameGlow.copy(alpha = 0.12f + 0.30f * framePulse)
                        ),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {}
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Transparent,
                        border = BorderStroke(
                            2.dp,
                            Color(0xFFFFE9A3).copy(alpha = 0.32f + 0.36f * framePulse)
                        ),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {}
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        shape = puzzleFrameShape,
                        color = puzzleFrameColor,
                        border = BorderStroke(
                            2.dp,
                            puzzleFrameBorder.copy(alpha = 0.94f)
                        ),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Box(
                            Modifier
                                .padding(1.dp)
                                .fillMaxSize()
                                .background(Color(0xFFFFF6E8), puzzleContentShape),
                            contentAlignment = Alignment.Center,
                            content = boardContent
                        )
                    }
                }
            }
            if (ui.solved) {
                Surface(
                    Modifier.fillMaxWidth(0.80f),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFE6F8F7),
                    border = BorderStroke(1.3.dp, Color(0xFF7ECBCB)),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!finishedReady) {
                            Text(
                                "Finishing your picture…",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = glassTealDark
                            )
                        } else {
                            Text(
                                "Adventure Finished!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = glassTealDark
                            )
                        }

                        if (ui.receipt != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GoldCoinIcon(Modifier.size(24.dp))
                                Text(
                                    "+${ui.receipt!!.awarded} coins",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "· ${formatPlayTime(ui.elapsedMillis)} · ${ui.moves} moves",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (ui.receipt!!.hintAwarded > 0) {
                                Text(
                                    "+${ui.receipt!!.hintAwarded} Hint · ${ui.receipt!!.hintBalance}/${GameSettings.MAX_HINTS} available",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            VibrantAction(
                                onClick = {
                                    when {
                                        replayMode -> onBack()
                                        levelId == PuzzleLevel.maxLevelId -> onHome()
                                        else -> onNextLevel(levelId + 1)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.52f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(22.dp),
                                brush = VibrantOrangeBrush,
                                motion = ActionMotion.BOUNCE_UP
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        when {
                                            replayMode -> "Back to History"
                                            levelId == PuzzleLevel.maxLevelId -> "Home"
                                            else -> "Level ${levelId + 1}"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    if (!replayMode && levelId != PuzzleLevel.maxLevelId) {
                                        Spacer(Modifier.width(5.dp))
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                                    }
                                }
                            }
                        } else if (!ui.saveError) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                "Saving your rewards…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    OceanTrayProgressBackground(
                        progress = animatedProgress,
                        modifier = Modifier.matchParentSize()
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                        PuzzleControlIcon(
                            icon = if (ui.running) Icons.Default.Pause else Icons.Default.PlayArrow,
                            label = if (ui.running) "Pause" else "Resume",
                            contentDescription = if (ui.running) "Pause the puzzle timer" else "Resume the puzzle timer",
                            enabled = !ui.loading && ui.image != null,
                            onClick = { if (ui.running) vm.pause() else vm.resume() },
                            modifier = Modifier.weight(1f),
                            brush = Brush.linearGradient(listOf(Color(0xFF6A5CFF), Color(0xFF9A6BFF))),
                            motion = ActionMotion.DEPTH_PRESS
                        )
                        PuzzleControlIcon(
                            icon = when {
                                gridShifted -> Icons.Default.Restore
                                canDecreaseGrid -> Icons.Default.GridView
                                else -> Icons.Default.CheckCircle
                            },
                            label = when {
                                gridShifted -> "Restore"
                                !canDecreaseGrid -> "Min Grid"
                                else -> "Grid"
                            },
                            contentDescription = when {
                                gridShifted -> "Restore the original puzzle grid for free"
                                !canDecreaseGrid -> "Minimum puzzle grid reached"
                                crystals > 0 -> "Spend one Crystal to reduce the grid and use fewer, larger pieces"
                                else -> "No Crystals remaining. Grid Shift is unavailable"
                            },
                            enabled = !ui.loading && ui.image != null && (gridShifted || canDecreaseGrid),
                            onClick = {
                                if (!gridShifted && canDecreaseGrid && crystals <= 0) crystalInfo = true
                                else vm.toggleGridShift()
                            },
                            modifier = Modifier.weight(1f),
                            containerColor = if (gridShifted) MaterialTheme.colorScheme.tertiaryContainer
                                else MaterialTheme.colorScheme.secondaryContainer,
                            brush = Brush.linearGradient(listOf(Color(0xFF20D5C7), Color(0xFF36B8FF))),
                            motion = ActionMotion.BOUNCE_UP
                        )
                        PuzzleControlIcon(
                            icon = Icons.Default.Lightbulb,
                            label = "Hint",
                            contentDescription = if (hints > 0)
                                "Use one Hint to solve one random puzzle step. $hints remaining"
                            else
                                "No Hints remaining. Earn a Hint by completing an Adventure within optimal moves",
                            enabled = hints > 0 && !ui.loading && ui.image != null && (!ui.started || ui.running),
                            badgeCount = hints,
                            onClick = {
                                vm.hintStep { result ->
                                    when (result) {
                                        1 -> {
                                            if (soundEnabled) feedback.playConnectionSound()
                                            if (hapticsEnabled) feedback.buzzForConnection()
                                        }
                                        2 -> {
                                            if (soundEnabled) feedback.playSolvedSound()
                                            if (hapticsEnabled) feedback.buzzForSolved()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            brush = Brush.linearGradient(listOf(Color(0xFFFFB739), Color(0xFFFF5F93))),
                            motion = ActionMotion.PULSE
                        )
                        PuzzleControlIcon(
                            icon = Icons.Default.Refresh,
                            label = "Restart",
                            contentDescription = "Restart this adventure",
                            enabled = !ui.loading && ui.image != null,
                            onClick = { confirmRestart = true },
                            modifier = Modifier.weight(1f),
                            brush = Brush.linearGradient(listOf(Color(0xFFFF795E), Color(0xFFFFA43C))),
                            motion = ActionMotion.SHAKE
                        )
                    }
                }
                }
            }
            ui.image?.attribution?.let { credit ->
                TextButton(onClick = { runCatching { uri.openUri(credit.photoUrl) } }) {
                    Text("Photo: ${credit.photographer} · Pexels", maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
            if (ui.saveError) {
                Text("Progress hasn’t been saved yet. Please retry before leaving.", color = MaterialTheme.colorScheme.error)
                VibrantAction(
                    onClick = vm::retrySave,
                    modifier = Modifier.height(48.dp).fillMaxWidth(0.5f),
                    brush = VibrantPinkBrush,
                    motion = ActionMotion.SHAKE
                ) { Text("Retry save", color = Color.White, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    if (confirmRestart) AlertDialog(onDismissRequest = { confirmRestart = false },
        title = { Text("Restart this adventure?") },
        text = { Text("This attempt’s moves and time will reset. Your collected coins are safe.") },
        confirmButton = { TextButton(onClick = { confirmRestart = false; vm.restart() }) { Text("Restart") } },
        dismissButton = { TextButton(onClick = { confirmRestart = false }) { Text("Keep playing") } })
    if (rewardInfo) AlertDialog(onDismissRequest = { rewardInfo = false },
        title = { Text("Your adventure coins") },
        text = { Text("Aim for ${formatPlayTime(CoinRewards.targetSeconds(currentGridSize) * 1000L)} and ${CoinRewards.parMoves(currentGridSize)} moves in this adventure. Earn coins for finishing, speed, and efficient moves. On a replay, earn the improvement over your previous best. Coins are a local game reward with no cash value.") },
        confirmButton = { TextButton(onClick = { rewardInfo = false }) { Text("Got it") } })
    if (crystalInfo) AlertDialog(onDismissRequest = { crystalInfo = false },
        icon = { CrystalIcon(28.dp) },
        title = { Text(if (crystals > 0) "Crystal Power" else "No Crystals left") },
        text = { Text(if (crystals > 0)
            "You have $crystals Crystal${if (crystals == 1) "" else "s"}. Grid Shift spends 1 Crystal to rebuild the current Adventure on a randomly smaller grid, one or two levels lower. Fewer cells create larger pieces and make the puzzle easier. Restoring the original grid is free."
        else
            "Your two free Journey Crystals have been used. Grid Shift cannot reduce the grid until more Crystals are purchased. The wallet is ready for Google Play Billing to credit purchased Crystals.") },
        confirmButton = { TextButton(onClick = { crystalInfo = false }) { Text("Got it") } })
}

private const val ADVENTURES_PER_MILESTONE = 4

private fun milestoneTitle(levelId: Int): String {
    val milestoneNumber = levelId / ADVENTURES_PER_MILESTONE
    return "${milestoneOrdinal(milestoneNumber)} Milestone Reached!"
}

private fun milestoneOrdinal(number: Int): String = when (number) {
    1 -> "First"
    2 -> "Second"
    3 -> "Third"
    4 -> "Fourth"
    5 -> "Fifth"
    6 -> "Sixth"
    7 -> "Seventh"
    8 -> "Eighth"
    9 -> "Ninth"
    10 -> "Tenth"
    11 -> "Eleventh"
    12 -> "Twelfth"
    13 -> "Thirteenth"
    14 -> "Fourteenth"
    15 -> "Fifteenth"
    16 -> "Sixteenth"
    17 -> "Seventeenth"
    18 -> "Eighteenth"
    19 -> "Nineteenth"
    20 -> "Twentieth"
    else -> "${number}${ordinalSuffix(number)}"
}

private fun ordinalSuffix(number: Int): String {
    val absNumber = kotlin.math.abs(number)
    val lastTwo = absNumber % 100
    if (lastTwo in 11..13) return "th"
    return when (absNumber % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}

@Composable
fun CoinPill(
    coins: Int,
    onClick: () -> Unit = {},
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val minHeight = if (compact) 40.dp else 52.dp
    val horizontal = if (compact) 10.dp else 16.dp
    val vertical = if (compact) 8.dp else 12.dp
    val iconSize = if (compact) 16.dp else 18.dp
    val textSize = if (compact) 15.sp else 18.sp
    VibrantAction(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = minHeight)
            .semantics(mergeDescendants = true) {
                contentDescription = "$coins gold coins. Reward details"
            },
        shape = RoundedCornerShape(if (compact) 18.dp else 22.dp),
        brush = Brush.linearGradient(listOf(Color(0xFFFFE07A), Color(0xFFFFB347))),
        contentColor = Color(0xFF5A3B00),
        motion = ActionMotion.SHRINK
    ) {
        Row(
            Modifier.padding(horizontal = horizontal, vertical = vertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            GoldCoinIcon(Modifier.size(iconSize))
            Spacer(Modifier.width(if (compact) 4.dp else 6.dp))
            Text(
                "$coins",
                fontWeight = FontWeight.ExtraBold,
                fontSize = textSize,
                color = Color(0xFF5A3B00)
            )
        }
    }
}

@Composable
fun CrystalPill(
    crystals: Int,
    onClick: () -> Unit = {},
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val minHeight = if (compact) 40.dp else 48.dp
    val horizontal = if (compact) 10.dp else 11.dp
    val vertical = if (compact) 8.dp else 10.dp
    val iconSize = if (compact) 17.dp else 20.dp
    val textSize = if (compact) 15.sp else 16.sp
    VibrantAction(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = minHeight)
            .semantics(mergeDescendants = true) {
                contentDescription = "$crystals crystals. Grid Shift power"
            },
        shape = RoundedCornerShape(if (compact) 18.dp else 22.dp),
        brush = Brush.linearGradient(listOf(Color(0xFF83E8FF), Color(0xFF50B9FF))),
        contentColor = Color(0xFF173B61),
        motion = ActionMotion.PULSE
    ) {
        Row(
            Modifier.padding(horizontal = horizontal, vertical = vertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CrystalIcon(iconSize)
            Spacer(Modifier.width(if (compact) 4.dp else 5.dp))
            Text(
                "$crystals",
                fontWeight = FontWeight.ExtraBold,
                fontSize = textSize,
                color = Color(0xFF173B61)
            )
        }
    }
}

@Composable
fun CrystalIcon(size: androidx.compose.ui.unit.Dp = 18.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val outer = Path().apply {
            moveTo(w * 0.50f, h * 0.04f)
            lineTo(w * 0.88f, h * 0.34f)
            lineTo(w * 0.69f, h * 0.91f)
            lineTo(w * 0.31f, h * 0.91f)
            lineTo(w * 0.12f, h * 0.34f)
            close()
        }
        drawPath(outer, color = Color(0xFF55C7F3))
        val shine = Path().apply {
            moveTo(w * 0.50f, h * 0.09f)
            lineTo(w * 0.73f, h * 0.35f)
            lineTo(w * 0.51f, h * 0.76f)
            lineTo(w * 0.34f, h * 0.36f)
            close()
        }
        drawPath(shine, color = Color(0xFFCFF5FF))
        drawLine(Color.White.copy(alpha = 0.9f), start = androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.34f), end = androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.34f), strokeWidth = (w * 0.055f).coerceAtLeast(1f))
    }
}

@Composable
private fun StandaloneTimer(
    elapsedMillis: Long,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFFF6FFFE), Color(0xFFE4F8F6))
                )
            )
            .border(1.2.dp, Color(0xFF83C9C8), shape)
            .semantics { contentDescription = "Timer ${formatPlayTime(elapsedMillis)}" },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formatPlayTime(elapsedMillis),
            color = Color(0xFF083F43),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            letterSpacing = 0.4.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun ResourceNotificationAction(
    value: Int,
    description: String,
    badgeColor: Color,
    motion: ActionMotion,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    // Keep the resource icons light and clean.  VibrantAction intentionally
    // casts a deep graphics-layer shadow for normal game buttons; on these
    // small HUD icons that shadow reads as a black halo, so the resource
    // controls use their own shadow-free translucent surface instead.
    Box(
        modifier = Modifier
            .width(60.dp)
            .height(54.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.72f))
                .border(1.dp, Color.White.copy(alpha = 0.96f), CircleShape)
                .clickable(onClick = onClick)
                .semantics { this.contentDescription = description },
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        val text = value.coerceAtMost(999999).toString()
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .widthIn(min = 22.dp, max = 56.dp)
                .heightIn(min = 20.dp),
            shape = CircleShape,
            color = badgeColor,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.95f))
        ) {
            Box(
                modifier = Modifier.padding(
                    horizontal = if (text.length >= 5) 4.dp else 6.dp,
                    vertical = 2.dp
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = Color(0xFF352400),
                    fontWeight = FontWeight.Black,
                    fontSize = when {
                        text.length >= 6 -> 9.sp
                        text.length >= 4 -> 10.sp
                        else -> 11.sp
                    },
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun PuzzleControlIcon(

    icon: ImageVector,
    label: String,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    brush: Brush = Brush.verticalGradient(listOf(containerColor.copy(alpha = 0.96f), Color.White.copy(alpha = 0.78f))),
    motion: ActionMotion = ActionMotion.SHRINK,
    badgeCount: Int? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        VibrantAction(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(58.dp)
                .semantics {
                    this.contentDescription = contentDescription
                },
            shape = CircleShape,
            brush = brush,
            motion = motion
        ) {
            if (badgeCount != null) {
                BadgedBox(
                    badge = {
                        Badge(containerColor = Color(0xFFFFF176), contentColor = Color(0xFF4A2C00)) {
                            Text(badgeCount.coerceAtLeast(0).toString(), fontWeight = FontWeight.Bold)
                        }
                    }
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(25.dp), tint = Color(0xFF083F43))
                }
            } else {
                Icon(icon, contentDescription = null, modifier = Modifier.size(25.dp), tint = Color(0xFF083F43))
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF0B5559),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OceanTrayProgressBackground(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val infiniteTransition = rememberInfiniteTransition(label = "ocean-tray-progress")
    val farWaveShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tray-far-wave-shift"
    )
    val frontWaveShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tray-front-wave-shift"
    )
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -0.25f,
        targetValue = 1.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tray-shimmer"
    )

    Canvas(
        modifier = modifier.background(Color(0xFFEAF8F5))
    ) {
        if (clampedProgress <= 0.001f) return@Canvas

        val waterTop = size.height * (1f - clampedProgress)
        val backAmplitude = 5.dp.toPx() * (0.55f + clampedProgress * 0.45f)
        val frontAmplitude = 8.dp.toPx() * (0.55f + clampedProgress * 0.45f)
        val backWaveLength = size.width / 1.05f
        val frontWaveLength = size.width / 1.45f

        fun buildWave(top: Float, amplitude: Float, waveLength: Float, shift: Float, verticalBias: Float): Path {
            return Path().apply {
                moveTo(0f, size.height)
                lineTo(0f, top)
                val steps = 96
                for (step in 0..steps) {
                    val x = size.width * step / steps.toFloat()
                    val angle = ((x / waveLength) * (Math.PI * 2.0)).toFloat() - (shift * (Math.PI * 2.0)).toFloat()
                    val y = top + kotlin.math.sin(angle) * amplitude + verticalBias * kotlin.math.sin(angle * 0.55f)
                    lineTo(x, y)
                }
                lineTo(size.width, size.height)
                close()
            }
        }

        val backWave = buildWave(waterTop + 7.dp.toPx(), backAmplitude, backWaveLength, farWaveShift, 1.6f)
        val frontWave = buildWave(waterTop, frontAmplitude, frontWaveLength, frontWaveShift, 2.8f)

        drawPath(
            path = backWave,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF9EF4EE).copy(alpha = 0.56f),
                    Color(0xFF54D8D3).copy(alpha = 0.72f),
                    Color(0xFF1AA3AE).copy(alpha = 0.82f)
                ),
                startY = waterTop,
                endY = size.height
            )
        )
        drawPath(
            path = frontWave,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFB1FFF4).copy(alpha = 0.90f),
                    Color(0xFF49DAD3).copy(alpha = 0.95f),
                    Color(0xFF0E8C98).copy(alpha = 0.98f)
                ),
                startY = waterTop,
                endY = size.height
            )
        )

        val crest = Path().apply {
            val steps = 96
            moveTo(0f, waterTop)
            for (step in 0..steps) {
                val x = size.width * step / steps.toFloat()
                val angle = ((x / frontWaveLength) * (Math.PI * 2.0)).toFloat() - (frontWaveShift * (Math.PI * 2.0)).toFloat()
                val y = waterTop + kotlin.math.sin(angle) * frontAmplitude + 2.8f * kotlin.math.sin(angle * 0.55f)
                lineTo(x, y)
            }
        }
        drawPath(
            path = crest,
            color = Color.White.copy(alpha = 0.92f),
            style = Stroke(width = 2.2.dp.toPx())
        )
        // foam glints carried from left to right
        repeat(4) { index ->
            val bubbleX = ((shimmerX + (index * 0.17f)) % 1.25f) * size.width
            drawCircle(
                color = Color.White.copy(alpha = 0.28f),
                radius = (2.2f + index * 0.55f).dp.toPx(),
                center = Offset(
                    bubbleX,
                    waterTop + 4.dp.toPx() + kotlin.math.sin(
                        (bubbleX / size.width) * (Math.PI * 2.0).toFloat() -
                            frontWaveShift * (Math.PI * 2.0).toFloat()
                    ).toFloat() * frontAmplitude
                )
            )
        }
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent,
                shimmerX.coerceIn(0f, 1f) to Color.White.copy(alpha = 0.09f),
                (shimmerX + 0.14f).coerceIn(0f, 1f) to Color.Transparent
            ),
            topLeft = Offset.Zero,
            size = size
        )
    }
}

@Composable
private fun FinishedImageTransformation(
    image: androidx.compose.ui.graphics.ImageBitmap,
    progress: Float,
    celebrationProgress: Float,
    modifier: Modifier = Modifier
) {
    val p = progress.coerceIn(0f, 1f)
    val frameShape = RoundedCornerShape(14.dp)
    val imageShape = RoundedCornerShape(10.dp)
    val labelShape = RoundedCornerShape(10.dp)
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFF6B6B),
            Color(0xFFFFC94A),
            Color(0xFF44D1C4),
            Color(0xFF6D8CFF),
            Color(0xFFC77DFF),
            Color(0xFFFF6B6B)
        )
    )

    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .fillMaxHeight(0.94f)
                .graphicsLayer {
                    alpha = p
                    val overshoot = when {
                        p < 0.72f -> 0.95f + (p / 0.72f) * 0.065f
                        else -> 1.015f - ((p - 0.72f) / 0.28f) * 0.015f
                    }
                    scaleX = overshoot
                    scaleY = overshoot
                    translationY = (1f - p) * 18f
                }
                .border(width = 3.dp, brush = borderBrush, shape = frameShape)
                .clip(frameShape)
                .background(Color(0xFFFFFBF5))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(imageShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = image,
                        contentDescription = "Finished picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .clip(labelShape)
                        .background(Color(0xFFF7EEE2)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FINISHED!",
                        modifier = Modifier.graphicsLayer {
                            val textProgress = ((p - 0.42f) / 0.58f).coerceIn(0f, 1f)
                            alpha = textProgress
                            scaleX = 0.88f + 0.12f * textProgress
                            scaleY = 0.88f + 0.12f * textProgress
                        },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFC78F88),
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        FinishedSparkleBurst(progress = celebrationProgress, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun FinishedSparkleBurst(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val t = progress.coerceIn(0f, 1f)
        if (t <= 0.001f || t >= 0.999f) return@Canvas

        val densityScale = density
        val frameLeft = size.width * 0.08f
        val frameTop = size.height * 0.04f
        val frameRight = size.width * 0.92f
        val frameBottom = size.height * 0.95f

        // Strong opening pop, then a slower fall so the celebration remains visible.
        val pop = (t / 0.20f).coerceIn(0f, 1f)
        val fade = if (t < 0.88f) 1f else (1f - ((t - 0.88f) / 0.12f)).coerceIn(0f, 1f)
        val visibleAlpha = pop * fade

        val palette = listOf(
            Color(0xFFFF5F6D), // coral
            Color(0xFFFFC83D), // yellow
            Color(0xFF41C7B7), // teal
            Color(0xFF5C7CFA), // blue
            Color(0xFFC86BFA), // purple
            Color(0xFF74D14C), // green
            Color(0xFFFF8FC7), // pink
            Color(0xFFFF8A3D)  // orange
        )

        // Big curled ribbons launched from both sides/top of the finished frame.
        val ribbonOrigins = listOf(
            Offset(frameLeft + 10f * densityScale, frameTop + 12f * densityScale),
            Offset(frameRight - 10f * densityScale, frameTop + 12f * densityScale),
            Offset(size.width * 0.33f, frameTop + 4f * densityScale),
            Offset(size.width * 0.67f, frameTop + 4f * densityScale)
        )
        ribbonOrigins.forEachIndexed { index, origin ->
            val side = if (index % 2 == 0) -1f else 1f
            val travel = (78f + index * 15f) * densityScale * t
            val wave = (28f + index * 4f) * densityScale
            val path = Path().apply {
                moveTo(origin.x, origin.y)
                cubicTo(
                    origin.x + side * wave,
                    origin.y + travel * 0.26f,
                    origin.x - side * wave * 0.80f,
                    origin.y + travel * 0.58f,
                    origin.x + side * wave * 0.38f,
                    origin.y + travel
                )
            }
            val ribbonColor = palette[index % palette.size]
            drawPath(
                path = path,
                color = ribbonColor.copy(alpha = visibleAlpha * 0.95f),
                style = Stroke(width = 5.0f * densityScale)
            )
            drawPath(
                path = path,
                color = Color.White.copy(alpha = visibleAlpha * 0.24f),
                style = Stroke(width = 1.3f * densityScale)
            )
        }

        // Large colorful paper pieces. Their starting points hug the frame, then burst
        // outward and fall with gravity. All values are deterministic so recomposition
        // produces a stable, fluid animation instead of flicker.
        repeat(54) { index ->
            val sideGroup = index % 4
            val fraction = ((index * 37) % 101) / 100f
            val start = when (sideGroup) {
                0 -> Offset(frameLeft, frameTop + (frameBottom - frameTop) * fraction)
                1 -> Offset(frameRight, frameTop + (frameBottom - frameTop) * fraction)
                2 -> Offset(frameLeft + (frameRight - frameLeft) * fraction, frameTop)
                else -> Offset(frameLeft + (frameRight - frameLeft) * fraction, frameBottom)
            }

            val horizontalSign = when (sideGroup) {
                0 -> -1f
                1 -> 1f
                else -> if (index % 2 == 0) -1f else 1f
            }
            val vx = horizontalSign * (28f + (index % 7) * 9f) * densityScale
            val initialVy = when (sideGroup) {
                2 -> -(50f + (index % 5) * 10f) * densityScale
                3 -> -(34f + (index % 4) * 8f) * densityScale
                else -> -(22f + (index % 6) * 7f) * densityScale
            }
            val gravity = (105f + (index % 5) * 11f) * densityScale
            val x = start.x + vx * t
            val y = start.y + initialVy * t + 0.5f * gravity * t * t

            val paperWidth = (5.5f + (index % 4) * 1.6f) * densityScale
            val paperHeight = (9.0f + (index % 3) * 2.8f) * densityScale
            val rotation = (index * 29f + t * (290f + (index % 5) * 35f)) % 360f
            val pieceAlpha = visibleAlpha * (0.78f + (index % 3) * 0.10f)
            val color = palette[index % palette.size].copy(alpha = pieceAlpha.coerceIn(0f, 1f))

            rotate(rotation, pivot = Offset(x, y)) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x - paperWidth / 2f, y - paperHeight / 2f),
                    size = Size(paperWidth, paperHeight),
                    cornerRadius = CornerRadius(1.7f * densityScale, 1.7f * densityScale)
                )
                drawLine(
                    color = Color.White.copy(alpha = pieceAlpha * 0.28f),
                    start = Offset(x - paperWidth * 0.28f, y - paperHeight * 0.30f),
                    end = Offset(x + paperWidth * 0.25f, y + paperHeight * 0.26f),
                    strokeWidth = (0.8f * densityScale).coerceAtLeast(1f)
                )
            }
        }

        // A handful of bright star flashes make the frame feel magical without relying
        // on border drawing.
        val starPoints = listOf(
            Offset(frameLeft + 12f * densityScale, frameTop + 12f * densityScale),
            Offset(frameRight - 14f * densityScale, frameTop + 20f * densityScale),
            Offset(frameLeft + 8f * densityScale, size.height * 0.52f),
            Offset(frameRight - 8f * densityScale, size.height * 0.48f),
            Offset(size.width * 0.28f, frameBottom - 9f * densityScale),
            Offset(size.width * 0.72f, frameBottom - 9f * densityScale)
        )
        starPoints.forEachIndexed { index, point ->
            val phase = ((t * 4.5f) + index * 0.18f) % 1f
            val twinkle = kotlin.math.sin((phase * Math.PI).toFloat()).coerceAtLeast(0f) * fade
            val radius = (3.0f + (index % 3) * 0.7f) * densityScale
            val c = if (index % 2 == 0) Color(0xFFFFE277) else Color.White
            drawCircle(c.copy(alpha = twinkle * 0.30f), radius = radius * 2.4f, center = point)
            drawLine(c.copy(alpha = twinkle), Offset(point.x - radius * 2.2f, point.y), Offset(point.x + radius * 2.2f, point.y), strokeWidth = 1.5f * densityScale)
            drawLine(c.copy(alpha = twinkle), Offset(point.x, point.y - radius * 2.2f), Offset(point.x, point.y + radius * 2.2f), strokeWidth = 1.5f * densityScale)
        }
    }
}

@Composable
private fun MergePraiseOverlay(
    mergeSize: Int,
    celebrationVersion: Int,
    solved: Boolean,
    modifier: Modifier = Modifier
) {
    val praise = mergePraiseWord(mergeSize, solved) ?: return
    val alpha = remember(celebrationVersion, praise) { Animatable(0f) }
    val scale = remember(celebrationVersion, praise) { Animatable(0.74f) }

    LaunchedEffect(celebrationVersion, praise) {
        alpha.snapTo(0f)
        scale.snapTo(0.74f)
        coroutineScope {
            launch {
                alpha.animateTo(1f, tween(120, easing = FastOutSlowInEasing))
                delay(220)
                alpha.animateTo(0f, tween(520, easing = FastOutSlowInEasing))
            }
            launch {
                scale.animateTo(1.10f, tween(150, easing = FastOutSlowInEasing))
                scale.animateTo(1.00f, tween(170, easing = FastOutSlowInEasing))
                delay(260)
                scale.animateTo(1.03f, tween(220, easing = FastOutSlowInEasing))
            }
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier.graphicsLayer {
                this.alpha = alpha.value
                scaleX = scale.value
                scaleY = scale.value
            },
            contentAlignment = Alignment.Center
        ) {
            val fontSize = when {
                praise.length >= 11 -> 29.sp
                praise.length >= 9 -> 32.sp
                else -> 36.sp
            }
            val outline = Color(0xFF9E3D18)
            val fill = Color(0xFFFFF8E9)
            val shadow = Color(0x6627130A)
            val outlineOffsets = listOf(
                -2.dp to -2.dp, 0.dp to -2.dp, 2.dp to -2.dp,
                -2.dp to 0.dp, 2.dp to 0.dp,
                -2.dp to 2.dp, 0.dp to 2.dp, 2.dp to 2.dp
            )

            Text(
                text = praise,
                color = shadow,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.offset(x = 1.dp, y = 4.dp)
            )
            outlineOffsets.forEach { (x, y) ->
                Text(
                    text = praise,
                    color = outline,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.offset(x = x, y = y)
                )
            }
            Text(
                text = praise,
                color = fill,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

private fun mergePraiseWord(mergeSize: Int, solved: Boolean): String? = when {
    solved && mergeSize >= 4 -> "MASTERPIECE!"
    mergeSize >= 13 -> "PERFECT!"
    mergeSize >= 9 -> "AMAZING!"
    mergeSize >= 6 -> "GREAT!"
    mergeSize >= 4 -> "NICE!"
    else -> null
}

internal fun formatPlayTime(millis: Long): String {
    val seconds = millis.coerceAtLeast(0) / 1000
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}
