package com.hb.puzz.ui

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hb.puzz.data.AdventureHistoryEntry
import com.hb.puzz.data.HomeSnapshot
import com.hb.puzz.domain.PuzzleLevel
import com.hb.puzz.ui.images.ImageAssets
import java.text.DateFormat
import java.util.Date

private const val JOURNAL_ADVENTURES_PER_MILESTONE = 4

private enum class JournalFilter { ALL, COMPLETED, MILESTONES }

@Composable
fun JourneyHistoryScreen(
    home: HomeSnapshot,
    onBack: () -> Unit,
    onOpenAdventure: (Int) -> Unit,
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
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(Modifier.weight(1f)) {
                Text("Journey Journal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Your Adventures, Milestones and personal stats",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                CrystalPill(home.crystals)
                CoinPill(home.coins)
            }
        }

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

        Text("Explore your journey", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            JournalFilterButton("All", filter == JournalFilter.ALL, Modifier.weight(1f)) {
                filterName = JournalFilter.ALL.name
            }
            JournalFilterButton("Completed", filter == JournalFilter.COMPLETED, Modifier.weight(1f)) {
                filterName = JournalFilter.COMPLETED.name
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
                        canReplay = completed && home.saved == null,
                        onToggle = {
                            expandedAdventure = if (expandedAdventure == level.id) null else level.id
                        },
                        onOpenAdventure = { onOpenAdventure(level.id) }
                    )
                }
                if (level.id % JOURNAL_ADVENTURES_PER_MILESTONE == 0) {
                    val number = level.id / JOURNAL_ADVENTURES_PER_MILESTONE
                    val reached = isMilestoneReached(number, home.completed)
                    if (filter == JournalFilter.ALL || (filter == JournalFilter.COMPLETED && reached)) {
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
private fun JourneySummaryCard(
    completed: Int,
    maxAdventure: Int,
    milestones: Int,
    progress: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f))
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Your Journey", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("$completed of $maxAdventure Adventures completed",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text("${(progress * 100).toInt()}%", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
            Box(
                Modifier.fillMaxWidth().height(10.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            ) {
                Box(
                    Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Text("$milestones Milestones reached",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatsGrid(moves: Int, playTime: Long, fastestTime: Long, wallet: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("TOTAL MOVES", if (moves > 0) moves.toString() else "—", Modifier.weight(1f))
            StatTile("PLAY TIME", if (playTime > 0) formatJournalDuration(playTime) else "—", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("FASTEST", if (fastestTime > 0) formatJournalDuration(fastestTime) else "—", Modifier.weight(1f))
            StatTile("GOLD COINS", wallet.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.66f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun JournalFilterButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = 44.dp),
            contentPadding = PaddingValues(horizontal = 6.dp),
            shape = CircleShape
        ) { Text(label, maxLines = 1) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 44.dp),
            contentPadding = PaddingValues(horizontal = 6.dp),
            shape = CircleShape
        ) { Text(label, maxLines = 1) }
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
    canReplay: Boolean,
    onToggle: () -> Unit,
    onOpenAdventure: () -> Unit
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
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Image(
                        painter = painterResource(ImageAssets.getLevelImage(level.id)),
                        contentDescription = "Adventure ${level.id} artwork",
                        modifier = Modifier.size(78.dp).clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                        alpha = if (unlocked) 1f else 0.48f
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
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("ADVENTURE ${level.id}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(level.title, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        when {
                            inProgress -> "In progress · tap for details"
                            completed -> "Completed · tap for stats"
                            ready -> "Ready for you"
                            else -> "Locked"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusDot(completed = completed, inProgress = inProgress, ready = ready)
            }

            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (history != null && (history.bestMoves > 0 || history.bestElapsedMillis > 0 || history.bestReward > 0)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiniStat("Best time", valueOrDash(history.bestElapsedMillis) { formatJournalDuration(it) }, Modifier.weight(1f))
                            MiniStat("Best moves", history.bestMoves.takeIf { it > 0 }?.toString() ?: "—", Modifier.weight(1f))
                            MiniStat("Best coins", history.bestReward.takeIf { it > 0 }?.toString() ?: "—", Modifier.weight(1f))
                        }
                        val completionText = if (history.completions == 1) "Completed once" else "Completed ${history.completions} times"
                        val date = history.lastCompletedAt.takeIf { it > 0 }?.let(::formatJournalDate)
                        Text(if (date != null) "$completionText · Last completed $date" else completionText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (completed) {
                        Text("Adventure completed. Detailed time and move history will be tracked from your next completion.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("${level.difficulty.displayLabel} · ${level.gridDescription}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    when {
                        inProgress -> Button(onClick = onOpenAdventure, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (completed) "Continue Replay" else "Continue Adventure")
                        }
                        ready -> Button(onClick = onOpenAdventure, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Start Adventure")
                        }
                        canReplay -> OutlinedButton(onClick = onOpenAdventure, modifier = Modifier.fillMaxWidth()) {
                            Text("Replay Adventure")
                        }
                        completed -> Text("Finish your current Adventure before starting a replay.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        completed -> "DONE"
        ready -> "READY"
        else -> "LOCKED"
    }
    val color = when {
        inProgress -> MaterialTheme.colorScheme.secondaryContainer
        completed -> MaterialTheme.colorScheme.primaryContainer
        ready -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    Surface(shape = CircleShape, color = color) {
        Text(text, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MilestoneJournalCard(number: Int, reached: Boolean) {
    val endAdventure = number * JOURNAL_ADVENTURES_PER_MILESTONE
    val startAdventure = endAdventure - JOURNAL_ADVENTURES_PER_MILESTONE + 1
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reached) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (reached) MaterialTheme.colorScheme.surface.copy(alpha = 0.76f)
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(Icons.Default.Star, contentDescription = null,
                    modifier = Modifier.padding(10.dp).size(22.dp),
                    tint = if (reached) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("${milestoneOrdinal(number)} Milestone",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (reached) "Reached after Adventure $endAdventure"
                    else "Complete Adventures $startAdventure–$endAdventure to reach this checkpoint",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(if (reached) "REACHED" else "AHEAD",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (reached) MaterialTheme.colorScheme.onTertiaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant)
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
