package com.hb.puzz.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hb.puzz.data.AdventureHistoryEntry
import com.hb.puzz.data.HomeSnapshot
import com.hb.puzz.domain.PuzzleLevel
import com.hb.puzz.ui.images.ImageAssets
import java.text.DateFormat
import java.util.Date

private const val JOURNAL_ADVENTURES_PER_MILESTONE = 4

private enum class JournalFilter { ALL, FINISHED, MILESTONES }

@Composable
fun JourneyHistoryScreen(
    home: HomeSnapshot,
    onBack: () -> Unit,
    onOpenAdventure: (Int) -> Unit,
    onReplayAdventure: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var filterName by rememberSaveable { mutableStateOf(JournalFilter.ALL.name) }
    var expandedAdventure by rememberSaveable { mutableStateOf<Int?>(null) }
    val filter = runCatching { JournalFilter.valueOf(filterName) }.getOrDefault(JournalFilter.ALL)
    val maxAdventure = PuzzleLevel.maxLevelId
    val historyById = remember(home.history) { home.history.associateBy { it.levelId } }
    val completedMilestones = milestoneCount(home.completed, maxAdventure)
    val totalTrackedMoves = home.history.sumOf { it.totalMoves }
    val totalTrackedTime = home.history.sumOf { it.totalElapsedMillis }
    val fastestTime = home.history.map { it.bestElapsedMillis }.filter { it > 0 }.minOrNull() ?: 0L
    val progress = (home.completed.size.toFloat() / maxAdventure.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFF3DB), Color(0xFFE9F8FF), Color(0xFFF5EAFF), Color(0xFFFFEEF4))
                )
            )
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        HistoryHeroCard(
            crystals = home.crystals,
            coins = home.coins,
            onBack = onBack
        )

        JourneySummaryCard(
            completed = home.completed.size,
            maxAdventure = maxAdventure,
            milestones = completedMilestones,
            progress = progress
        )

        StatsGrid(
            moves = totalTrackedMoves,
            playTime = totalTrackedTime,
            fastestTime = fastestTime,
            wallet = home.coins
        )

        SectionHeader(
            title = "Past Adventures",
            subtitle = "Tap a card"
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            JournalFilterButton("All", filter == JournalFilter.ALL, Modifier.weight(1f)) {
                filterName = JournalFilter.ALL.name
            }
            JournalFilterButton("Finished", filter == JournalFilter.FINISHED, Modifier.weight(1f)) {
                filterName = JournalFilter.FINISHED.name
            }
            JournalFilterButton("Milestones", filter == JournalFilter.MILESTONES, Modifier.weight(1f)) {
                filterName = JournalFilter.MILESTONES.name
            }
        }

        if (filter != JournalFilter.MILESTONES) {
            PuzzleLevel.ALL_LEVELS.forEach { level ->
                val completed = level.id in home.completed
                if (filter == JournalFilter.ALL || completed) {
                    val inProgress = home.saved?.levelId == level.id
                    val ready = !completed && !inProgress && home.saved == null && level.id == home.highest
                    val unlocked = completed || inProgress || ready
                    AdventureJournalCard(
                        level = level,
                        history = historyById[level.id],
                        completed = completed,
                        inProgress = inProgress,
                        ready = ready,
                        unlocked = unlocked,
                        expanded = expandedAdventure == level.id,
                        onToggle = {
                            expandedAdventure = if (expandedAdventure == level.id) null else level.id
                        },
                        onOpenAdventure = { onOpenAdventure(level.id) },
                        onReplayAdventure = { onReplayAdventure(level.id) }
                    )
                }
                if (level.id % JOURNAL_ADVENTURES_PER_MILESTONE == 0) {
                    val number = level.id / JOURNAL_ADVENTURES_PER_MILESTONE
                    val reached = isMilestoneReached(number, home.completed)
                    if (filter == JournalFilter.ALL || (filter == JournalFilter.FINISHED && reached)) {
                        MilestoneJournalCard(number = number, reached = reached)
                    }
                }
            }
        } else {
            val count = (maxAdventure + JOURNAL_ADVENTURES_PER_MILESTONE - 1) / JOURNAL_ADVENTURES_PER_MILESTONE
            (1..count).forEach { number ->
                MilestoneJournalCard(number = number, reached = isMilestoneReached(number, home.completed))
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun HistoryHeroCard(
    crystals: Int,
    coins: Int,
    onBack: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(30.dp), clip = false),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFF7FCFF), Color(0xFFF1F7FF), Color(0xFFFAF1FF))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                VibrantCircleAction(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp),
                    brush = VibrantBlueBrush,
                    motion = ActionMotion.SHRINK
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HistoryResourceMiniButton(
                        value = crystals,
                        description = "$crystals crystals",
                        badgeColor = Color(0xFF47D1F4),
                        motion = ActionMotion.PULSE,
                        icon = { CrystalIcon(24.dp) }
                    )
                    HistoryResourceMiniButton(
                        value = coins,
                        description = "$coins gold coins",
                        badgeColor = Color(0xFFFFC443),
                        motion = ActionMotion.BOUNCE_UP,
                        icon = { GoldCoinIcon(Modifier.size(24.dp)) }
                    )
                }
            }

            Text(
                "Adventure History",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = Color(0xFF223458),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = MaterialTheme.typography.displaySmall.lineHeight
            )

            HeroPill(text = "Your journey")
        }
    }
}

@Composable
private fun HeroPill(text: String) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, Color(0xFFF0F4FF))
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF496078)
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = Color(0xFF2B3554)
        )
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7791))
        }
    }
}

@Composable
private fun HistoryResourceMiniButton(
    value: Int,
    description: String,
    badgeColor: Color,
    motion: ActionMotion,
    icon: @Composable () -> Unit
) {
    val text = value.coerceAtMost(999999).toString()
    Box(
        modifier = Modifier
            .width(54.dp)
            .height(54.dp),
        contentAlignment = Alignment.Center
    ) {
        VibrantAction(
            onClick = {},
            modifier = Modifier
                 .size(42.dp)
                .semantics { contentDescription = description },
            shape = CircleShape,
            brush = Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF1F3FF))),
            motion = motion
        ) {
            icon()
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .widthIn(min = 22.dp, max = 54.dp)
                .heightIn(min = 20.dp),
            shape = CircleShape,
            color = badgeColor,
            shadowElevation = 3.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.90f))
        ) {
            Box(
                modifier = Modifier.padding(horizontal = if (text.length >= 5) 4.dp else 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = Color(0xFF27303B),
                    fontWeight = FontWeight.Black,
                    fontSize = when {
                        text.length >= 6 -> 8.sp
                        text.length >= 4 -> 9.sp
                        else -> 10.sp
                    },
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun JourneySummaryCard(
    completed: Int,
    maxAdventure: Int,
    milestones: Int,
    progress: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.82f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(Color(0xFFDFF4FF), Color(0xFFF2EEFF), Color(0xFFFFF7E9)))
                )
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Progress",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF223458)
                    )
                    Text(
                        "${completed} / ${maxAdventure}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF647189)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Color(0xFFE6EDFA))
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = Color(0xFF345187),
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.66f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceAtLeast(0.06f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF60D8FF), Color(0xFF6E82FF), Color(0xFFFF92BE))
                            )
                        )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryBadge("✓", completed.toString(), Modifier.weight(1f), Color(0xFFEAF8FF), Color(0xFF2F6C8E))
                SummaryBadge("★", milestones.toString(), Modifier.weight(1f), Color(0xFFFFF4D9), Color(0xFF8D6A1E))
                SummaryBadge("↗", (maxAdventure - completed).coerceAtLeast(0).toString(), Modifier.weight(1f), Color(0xFFFFE5F0), Color(0xFFA34774))
            }
        }
    }
}

@Composable
private fun SummaryBadge(
    label: String,
    value: String,
    modifier: Modifier,
    color: Color,
    valueColor: Color
) {
    Card(
        modifier = modifier.height(74.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = valueColor
            )
        }
    }
}

@Composable
private fun StatsGrid(moves: Int, playTime: Long, fastestTime: Long, wallet: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("TOTAL MOVES", if (moves > 0) moves.toString() else "—", Modifier.weight(1f), Color(0xFFFFF7E2), Color(0xFF91681B))
            StatTile("PLAY TIME", if (playTime > 0) formatJournalDuration(playTime) else "—", Modifier.weight(1f), Color(0xFFE9F7FF), Color(0xFF2F688B))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("FASTEST", if (fastestTime > 0) formatJournalDuration(fastestTime) else "—", Modifier.weight(1f), Color(0xFFEAF8EA), Color(0xFF33794B))
            StatTile("GOLD COINS", wallet.toString(), Modifier.weight(1f), Color(0xFFFFEAF5), Color(0xFF99446F))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier, background: Color, valueColor: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.85f))
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = valueColor.copy(alpha = 0.85f), fontWeight = FontWeight.Black)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = valueColor)
        }
    }
}

@Composable
private fun JournalFilterButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    VibrantAction(
        onClick = onClick,
        modifier = modifier.heightIn(min = 46.dp),
        shape = CircleShape,
        brush = if (selected) VibrantBlueBrush else Brush.linearGradient(
            listOf(Color(0xFFFFD7E5), Color(0xFFFFE7B8), Color(0xFFDDF7F0))
        ),
        contentColor = if (selected) Color.White else Color(0xFF563B48),
        motion = ActionMotion.SHRINK
    ) {
        Text(label, maxLines = 1, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AdventureJournalCard(
    level: PuzzleLevel,
    history: AdventureHistoryEntry?,
    completed: Boolean,
    inProgress: Boolean,
    ready: Boolean,
    unlocked: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenAdventure: () -> Unit,
    onReplayAdventure: () -> Unit
) {
    val container = when {
        inProgress -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.82f)
        completed -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
        ready -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.50f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    }
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Image(
                        painter = painterResource(ImageAssets.getLevelImage(level.id)),
                        contentDescription = "Adventure ${level.id} artwork",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .then(if (!unlocked) Modifier.blur(18.dp) else Modifier),
                        contentScale = ContentScale.Crop,
                        alpha = if (unlocked) 1f else 0.58f
                    )
                    if (!unlocked) {
                        Surface(
                            modifier = Modifier.align(Alignment.Center),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Locked",
                                modifier = Modifier.padding(8.dp).size(18.dp))
                        }
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("A${level.id}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(level.title, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        when {
                            inProgress -> "In progress"
                            completed -> "Finished"
                            ready -> "Ready"
                            else -> "Locked"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusDot(completed = completed, inProgress = inProgress, ready = ready)
            }

            if (completed && !inProgress) {
                VibrantAction(
                    onClick = onReplayAdventure,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                        .height(48.dp),
                    shape = CircleShape,
                    brush = VibrantPinkBrush,
                    motion = ActionMotion.BOUNCE_UP
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Replay Adventure", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (history != null && (history.bestMoves > 0 || history.bestElapsedMillis > 0 || history.bestReward > 0)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiniStat("Best time", valueOrDash(history.bestElapsedMillis) { formatJournalDuration(it) }, Modifier.weight(1f))
                            MiniStat("Best moves", history.bestMoves.takeIf { it > 0 }?.toString() ?: "—", Modifier.weight(1f))
                            MiniStat("Best coins", history.bestReward.takeIf { it > 0 }?.toString() ?: "—", Modifier.weight(1f))
                        }
                        val completionText = if (history.completions == 1) "Finished once" else "Finished ${history.completions} times"
                        val date = history.lastCompletedAt.takeIf { it > 0 }?.let(::formatJournalDate)
                        Text(if (date != null) "$completionText · Last completed $date" else completionText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (completed) {
                        Text("Adventure finished. Detailed time and move history will be tracked from your next finish.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("${level.difficulty.displayLabel} · ${level.gridDescription}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    when {
                        inProgress -> VibrantAction(
                            onClick = onOpenAdventure,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = CircleShape,
                            brush = VibrantBlueBrush,
                            motion = ActionMotion.BOUNCE_UP
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text(if (completed) "Continue Replay" else "Continue Adventure", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        ready -> VibrantAction(
                            onClick = onOpenAdventure,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = CircleShape,
                            brush = VibrantMintBrush,
                            motion = ActionMotion.BOUNCE_UP
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text("Start Adventure", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDot(completed: Boolean, inProgress: Boolean, ready: Boolean) {
    val text = when {
        inProgress -> "PLAYING"
        completed -> "FINISHED"
        ready -> "READY"
        else -> "LOCKED"
    }
    val brush = when {
        inProgress -> Brush.linearGradient(listOf(Color(0xFF73D8FF), Color(0xFF3B9DFF)))
        completed -> Brush.linearGradient(listOf(Color(0xFFFFC3D8), Color(0xFFFF8FB0)))
        ready -> Brush.linearGradient(listOf(Color(0xFFFFE38C), Color(0xFFFFB74D)))
        else -> Brush.linearGradient(listOf(Color(0xFFE5E7EC), Color(0xFFB0B7C4)))
    }
    Surface(shape = CircleShape, color = Color.Transparent) {
        Box(
            Modifier
                .clip(CircleShape)
                .background(brush)
        ) {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = if (inProgress || completed) Color.White else Color(0xFF4A505C)
            )
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.42f))) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black, color = Color(0xFF32405E))
        }
    }
}

@Composable
private fun MilestoneJournalCard(number: Int, reached: Boolean) {
    val endAdventure = number * JOURNAL_ADVENTURES_PER_MILESTONE
    val startAdventure = endAdventure - JOURNAL_ADVENTURES_PER_MILESTONE + 1
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reached) Color(0xFFFFF0BF) else Color(0xFFF4F0FB)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (reached) MaterialTheme.colorScheme.surface.copy(alpha = 0.76f)
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = if (reached) "★" else "☆",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Black,
                    color = if (reached) Color(0xFF8D6A1E) else Color(0xFF70778A)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "M$number",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF37435B)
                )
                Text(
                    text = "$startAdventure-$endAdventure",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (reached) "✓" else "…",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = if (reached) Color(0xFF4B8B4F) else Color(0xFF7C8599)
            )
        }
    }
}

private fun milestoneCount(completed: Set<Int>, maxAdventure: Int): Int {
    val count = (maxAdventure + JOURNAL_ADVENTURES_PER_MILESTONE - 1) / JOURNAL_ADVENTURES_PER_MILESTONE
    return (1..count).count { isMilestoneReached(it, completed) }
}

private fun isMilestoneReached(number: Int, completed: Set<Int>): Boolean {
    val end = number * JOURNAL_ADVENTURES_PER_MILESTONE
    val start = end - JOURNAL_ADVENTURES_PER_MILESTONE + 1
    return (start..end).all { it in completed }
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
    else -> "$number${ordinalSuffix(number)}"
}

private fun ordinalSuffix(number: Int): String {
    val n = kotlin.math.abs(number)
    if (n % 100 in 11..13) return "th"
    return when (n % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}

private fun formatJournalDuration(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0) / 1000L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

private inline fun <T> valueOrDash(value: Long, formatter: (Long) -> T): String =
    if (value > 0) formatter(value).toString() else "—"

private fun formatJournalDate(epochMillis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMillis))
