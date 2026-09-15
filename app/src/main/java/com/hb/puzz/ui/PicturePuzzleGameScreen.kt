package com.hb.puzz.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.window.Dialog
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
import com.hb.puzz.domain.MergeMotion
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
    var resultsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(ui.receipt) {
        resultsVisible = false
        if (ui.receipt != null) { delay(MergeMotion.RESULT_DELAY_MILLIS); resultsVisible = true }
    }
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

    val pleasantSkyBlue = Color(0xFFEAF7FF)
    val softPanel = Color(0xFFF8FCFF)

    BoxWithConstraints(modifier.fillMaxSize().background(pleasantSkyBlue).safeDrawingPadding()) {
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
            // Keep the frame OUTSIDE the artwork instead of painting a border on top of it.
            // A 1dp frame avoids the previous overlap where the rounded stroke covered the
            // image corners and merged-block outlines. The smaller radius keeps the board
            // rectangular and puzzle-like instead of looking like an oval card.
            val puzzleFrameShape = RoundedCornerShape(7.dp)
            val puzzleContentShape = RoundedCornerShape(6.dp)
            val puzzleFrameColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.52f)
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
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        ui.loading -> CircularProgressIndicator()
                        ui.error != null -> Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(ui.error!!)
                            Button(onClick = vm::load) { Text("Try again") }
                        }
                        ui.solved && ui.celebration.isEmpty() && ui.image != null -> Image(
                            ui.image!!.bitmap.asImageBitmap(),
                            "Completed picture",
                            Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                        ui.image != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            PuzzleBoard(vm.engine, ui.boardVersion, ui.image!!.bitmap.asImageBitmap(),
                                ui.celebration, ui.celebrationVersion,
                                onGroupDropped = { anchor, target ->
                                    when (vm.drop(anchor, target)) {
                                        1 -> { if (soundEnabled) feedback.playConnectionSound(); if (hapticsEnabled) feedback.buzzForConnection() }
                                        2 -> { if (soundEnabled) feedback.playSolvedSound(); if (hapticsEnabled) feedback.buzzForSolved() }
                                    }
                                }, modifier = Modifier.fillMaxSize(), inputEnabled = !ui.solved && (!ui.started || ui.running))
                            MergePraiseOverlay(
                                mergeSize = ui.celebration.size,
                                celebrationVersion = ui.celebrationVersion,
                                solved = ui.solved,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                color = softPanel) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Completion", style = MaterialTheme.typography.labelLarge)
                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        trackColor = Color.White)

                    // Four compact actions directly under the grid:
                    // timer -> Crystal Grid Shift -> one-step Hint -> Restart.
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        PuzzleControlIcon(
                            icon = if (ui.running) Icons.Default.Pause else Icons.Default.PlayArrow,
                            label = if (ui.running) "Pause" else "Resume",
                            contentDescription = if (ui.running)
                                "Pause the puzzle timer"
                            else
                                "Resume the puzzle timer",
                            enabled = !ui.loading && !ui.solved && ui.image != null,
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
                            enabled = !ui.loading && !ui.solved && ui.image != null && (gridShifted || canDecreaseGrid),
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
                            enabled = hints > 0 && !ui.loading && !ui.solved && ui.image != null && (!ui.started || ui.running),
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
                            enabled = !ui.loading && !ui.solved && ui.image != null,
                            onClick = { confirmRestart = true },
                            modifier = Modifier.weight(1f)
                        )
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
            if (ui.solved && ui.receipt == null && !ui.saveError) Text("Saving your coins…")
            if (ui.receipt != null) Button(onClick = { resultsVisible = true }) { Text("View earned coins") }
            Spacer(Modifier.height(8.dp))
        }
    }
    if (resultsVisible && ui.receipt != null) Dialog(onDismissRequest = { resultsVisible = false }) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            RewardCard(ui.receipt!!.awarded, ui.receipt!!.reward.completion, ui.receipt!!.reward.speed,
                ui.receipt!!.reward.efficiency, ui.elapsedMillis, ui.moves, ui.speedEligible,
                hintAwarded = ui.receipt!!.hintAwarded,
                hintBalance = ui.receipt!!.hintBalance,
                completionTitle = when {
                    levelId % ADVENTURES_PER_MILESTONE == 0 -> milestoneTitle(levelId)
                    else -> "Adventure Complete!"
                },
                nextLabel = if (levelId == PuzzleLevel.maxLevelId) "Finish Journey" else "Next Adventure",
                onNext = { if (levelId == PuzzleLevel.maxLevelId) onHome() else onNextLevel(levelId + 1) })
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
private fun RewardCard(awarded: Int, base: Int, speed: Int, movesBonus: Int, elapsed: Long, moves: Int,
    speedEligible: Boolean, hintAwarded: Int, hintBalance: Int,
    completionTitle: String, nextLabel: String, onNext: () -> Unit) {
    var coinTarget by remember(awarded) { mutableIntStateOf(0) }
    LaunchedEffect(awarded) { coinTarget = awarded }
    val animatedCoins by animateIntAsState(coinTarget,
        animationSpec = androidx.compose.animation.core.tween(750), label = "earned-coins")
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(completionTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldCoinIcon(Modifier.size(32.dp))
                Text("+$animatedCoins coins", style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
            }
            Text("${formatPlayTime(elapsed)} active time · $moves moves")
            if (hintAwarded > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "+$hintAwarded Hint · $hintBalance/${GameSettings.MAX_HINTS} available",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text("Completion $base  +  Speed $speed  +  Moves $movesBonus", style = MaterialTheme.typography.bodySmall)
            if (!speedEligible) Text("This older save has no reliable time record, so no speed bonus applies.", style = MaterialTheme.typography.bodySmall)
            if (awarded < base + speed + movesBonus) Text("Replay coins reflect the improvement over your best for this adventure.", style = MaterialTheme.typography.bodySmall)
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(nextLabel) }
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
