package com.hb.puzz.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    modifier: Modifier = Modifier
) {
    val factory = remember(levelId, gridSize) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PuzzleGameViewModel(levelId, gridSize, settings, imageRepository, imageSource) as T
        }
    }
    val vm: PuzzleGameViewModel = viewModel(key = "puzzle-$levelId-$gridSize", factory = factory)
    val ui by vm.state.collectAsStateWithLifecycle()
    val wallet by settings.coinBalanceFlow.collectAsStateWithLifecycle(initialValue = 0)
    val crystals by settings.crystalBalanceFlow.collectAsStateWithLifecycle(initialValue = GameSettings.INITIAL_CRYSTALS)
    val hints by settings.hintBalanceFlow.collectAsStateWithLifecycle(initialValue = GameSettings.INITIAL_HINTS)
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
    LaunchedEffect(finishedTarget) {
        if (finishedTarget) {
            celebrationBurst.snapTo(0f)
            delay(140)
            celebrationBurst.animateTo(
                targetValue = 1f,
                animationSpec = tween(1950, easing = LinearOutSlowInEasing)
            )
        } else {
            celebrationBurst.snapTo(0f)
        }
    }
    val uri = LocalUriHandler.current

    DisposableEffect(lifecycle, vm) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                vm.pause()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); vm.pause() }
    }
    DisposableEffect(feedback) { onDispose { feedback.release() } }
    BackHandler { vm.saveAndLeave(onBack) }

    val pleasantCream = Color(0xFFFFF7EA)
    val softPanel = Color(0xFFFFFCF4)

    BoxWithConstraints(modifier.fillMaxSize().background(pleasantCream).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { vm.saveAndLeave(onBack) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Save and go back")
                }
                CompactStatCard("TIME", formatPlayTime(ui.elapsedMillis), Modifier.weight(0.92f), softPanel)
                CompactStatCard("MOVES", "${ui.moves}", Modifier.weight(0.92f), softPanel)
                CompactWalletCard(
                    crystals = crystals,
                    coins = wallet,
                    modifier = Modifier.weight(1.56f),
                    background = softPanel,
                    onCrystalClick = { crystalInfo = true },
                    onCoinClick = { rewardInfo = true }
                )
            }
            // Keep the live puzzle at the exact source-image aspect ratio.
            // Using a fixed/taller ratio stretches the bitmap slices and makes the outer
            // corners and merged contours look misaligned with the artwork.
            val puzzleAspectRatio = ui.image?.bitmap?.let { bitmap ->
                if (bitmap.height > 0) bitmap.width.toFloat() / bitmap.height.toFloat() else 1f
            } ?: 1f
            // Keep the live puzzle framed while playing, but remove that outer board frame
            // completely during the magical finished transformation so no extra oval/outline
            // remains behind the final finished card.
            val puzzleFrameShape = RoundedCornerShape(7.dp)
            val puzzleContentShape = RoundedCornerShape(6.dp)
            val puzzleFrameColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.52f)
            val boardContent: @Composable BoxScope.() -> Unit = {
                when {
                    ui.loading -> CircularProgressIndicator()
                    ui.error != null -> Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(ui.error!!)
                        Button(onClick = vm::load) { Text("Try again") }
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
                        .fillMaxWidth()
                        .aspectRatio(puzzleAspectRatio),
                    contentAlignment = Alignment.Center,
                    content = boardContent
                )
            } else {
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = puzzleFrameShape,
                    color = puzzleFrameColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 3.dp
                ) {
                    Box(
                        Modifier
                            .padding(1.dp)
                            .fillMaxWidth()
                            .aspectRatio(puzzleAspectRatio)
                            .clip(puzzleContentShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center,
                        content = boardContent
                    )
                }
            }
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = softPanel
            ) {
                if (ui.solved) {
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
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                "Adventure Finished!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
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
                            Button(
                                onClick = {
                                    if (levelId == PuzzleLevel.maxLevelId) onHome()
                                    else onNextLevel(levelId + 1)
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.52f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFA800),
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Text(
                                    if (levelId == PuzzleLevel.maxLevelId) "Home" else "Level ${levelId + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                if (levelId != PuzzleLevel.maxLevelId) {
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
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
                } else {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Completion", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            trackColor = Color.White
                        )

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            PuzzleControlIcon(
                                icon = if (ui.running) Icons.Default.Pause else Icons.Default.PlayArrow,
                                label = if (ui.running) "Pause" else "Resume",
                                contentDescription = if (ui.running) "Pause the puzzle timer" else "Resume the puzzle timer",
                                enabled = !ui.loading && ui.image != null,
                                onClick = { if (ui.running) vm.pause() else vm.resume() },
                                modifier = Modifier.weight(1f)
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
                                    else MaterialTheme.colorScheme.secondaryContainer
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
                                modifier = Modifier.weight(1f)
                            )
                            PuzzleControlIcon(
                                icon = Icons.Default.Refresh,
                                label = "Restart",
                                contentDescription = "Restart this adventure",
                                enabled = !ui.loading && ui.image != null,
                                onClick = { confirmRestart = true },
                                modifier = Modifier.weight(1f)
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
                OutlinedButton(onClick = vm::retrySave) { Text("Retry save") }
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
fun CoinPill(coins: Int, onClick: () -> Unit = {}) {
    Surface(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp).semantics(mergeDescendants = true) {
        contentDescription = "$coins gold coins. Reward details"
    }, shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            GoldCoinIcon()
            Spacer(Modifier.width(4.dp))
            Text("$coins", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
fun CrystalPill(crystals: Int, onClick: () -> Unit = {}) {
    Surface(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp).semantics(mergeDescendants = true) {
        contentDescription = "$crystals crystals. Grid Shift power"
    }, shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            CrystalIcon(20.dp)
            Spacer(Modifier.width(4.dp))
            Text("$crystals", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
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
private fun CompactStatCard(
    label: String,
    value: String,
    modifier: Modifier,
    background: Color
) {
    Surface(modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp), color = background) {
        Column(
            Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CompactWalletCard(
    crystals: Int,
    coins: Int,
    modifier: Modifier,
    background: Color,
    onCrystalClick: () -> Unit,
    onCoinClick: () -> Unit
) {
    Surface(modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp), color = background) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onCrystalClick,
                color = Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .semantics { contentDescription = "$crystals crystals. Grid Shift power" }
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Row(
                        Modifier.padding(horizontal = 5.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CrystalIcon(20.dp)
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "$crystals",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
            }
            Box(
                Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Surface(
                onClick = onCoinClick,
                color = Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .semantics { contentDescription = "$coins gold coins. Reward details" }
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Row(
                        Modifier.padding(horizontal = 5.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        GoldCoinIcon(Modifier.size(20.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "$coins",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
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
    badgeCount: Int? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(48.dp)
                .semantics { this.contentDescription = contentDescription },
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = containerColor)
        ) {
            if (badgeCount != null) {
                BadgedBox(
                    badge = {
                        Badge {
                            Text(badgeCount.coerceAtLeast(0).toString())
                        }
                    }
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(23.dp))
                }
            } else {
                Icon(icon, contentDescription = null, modifier = Modifier.size(23.dp))
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
        val fade = if (t < 0.72f) 1f else (1f - ((t - 0.72f) / 0.28f)).coerceIn(0f, 1f)
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
