package com.hb.puzz.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.hb.puzz.data.BlockMotionSpeed
import com.hb.puzz.data.images.ImageSourceMode

private val HomeCream = Color(0xFFFFF7EA)
private val HomePanel = Color(0xFFFFFCF4)
private val HomeAccent = Color(0xFFB86A3B)
private val HomeAccentSoft = Color(0xFFF4E1C9)
private val HomeAccentDeep = Color(0xFF8D4F23)
private val TitlePanelTop = Color(0xFF5B3524)
private val TitlePanelBottom = Color(0xFF2E1B13)
private val TitleLight = Color(0xFFFFF5E9)
private val TitleWarm = Color(0xFFFFD39C)

@Composable
fun HomeScreen(
    crystalBalance: Int,
    coinBalance: Int,
    completedCount: Int,
    hasSavedGame: Boolean,
    currentChapter: Int,
    currentChapterTitle: String,
    currentDifficulty: String,
    currentGridDescription: String,
    journeyComplete: Boolean,
    onContinue: () -> Unit,
    onStartJourney: () -> Unit,
    onJourneyHistory: () -> Unit,
    onHowToPlay: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryAction = if (hasSavedGame) onContinue else onStartJourney
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFDBEEF8),
                        Color(0xFFEAF8FF),
                        Color(0xFFF4EFE7),
                        Color(0xFFE0F1EA)
                    )
                )
            )
            .safeDrawingPadding()
    ) {
        HomeBackgroundLandscapePuzzle()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SoftChromeIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Settings",
                    onClick = onSettings
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopResourcePill(
                        icon = { GoldCoinIcon(Modifier.size(22.dp)) },
                        value = coinBalance,
                        tintColor = Color(0xFF7A5200),
                        background = Brush.horizontalGradient(listOf(Color(0xFFFFF2B0), Color(0xFFFFD76A))),
                        onClick = onHowToPlay,
                        motion = ActionMotion.BOUNCE_UP
                    )
                    TopResourcePill(
                        icon = { CrystalIcon(20.dp) },
                        value = crystalBalance,
                        tintColor = Color(0xFF1E4666),
                        background = Brush.horizontalGradient(listOf(Color(0xFFD8F7FF), Color(0xFF9FE4FF))),
                        onClick = onHowToPlay,
                        motion = ActionMotion.PULSE
                    )
                }
            }

            Spacer(Modifier.weight(0.75f))

            SnapFrameLogo()
            Spacer(Modifier.height(18.dp))

            PremiumPlayLevelCallToAction(
                label = "PLAY",
                currentChapter = currentChapter,
                title = currentChapterTitle,
                onClick = primaryAction
            )

            Spacer(Modifier.weight(1f))
        }

        HistoryFloatingButton(
            onClick = onJourneyHistory,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 20.dp)
        )
    }
}

@Composable
private fun HomeBackgroundLandscapePuzzle() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 88.dp)
                .fillMaxWidth(0.92f)
                .aspectRatio(0.78f)
                .rotate(-6f)
                .clip(RoundedCornerShape(36.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x55FFFFFF), Color(0x55BFE8FF), Color(0x66D6F0E3), Color(0x66E7D8B4))
                    )
                )
        ) {
            Column(Modifier.fillMaxSize()) {
                repeat(6) { row ->
                    Row(Modifier.weight(1f)) {
                        repeat(4) { col ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(
                                        when ((row + col) % 4) {
                                            0 -> Color(0x22FFFFFF)
                                            1 -> Color(0x228ED4FF)
                                            2 -> Color(0x22A2E5C5)
                                            else -> Color(0x22F6E3A8)
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.88f)
                .aspectRatio(0.78f)
                .clip(RoundedCornerShape(40.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x24FFFFFF), Color(0x32DFF1FF), Color(0x28D3E8F0), Color(0x2DE2D2B6))
                    )
                )
        )
    }
}

@Composable
private fun SnapFrameLogo() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "SnapFrame",
            color = Color(0xFF26435B),
            fontSize = 44.sp,
            lineHeight = 48.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp
        )
        Text(
            text = "Relax. Rebuild. Reveal.",
            color = Color(0xFF5E7C8E),
            style = MaterialTheme.typography.titleMedium,
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
private fun PremiumPlayLevelCallToAction(
    label: String,
    currentChapter: Int,
    title: String,
    onClick: () -> Unit
) {
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateIn = true }

    val cardScale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.84f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 320f),
        label = "playCardScale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "playCardAlpha"
    )
    val badgeScale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.70f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 360f),
        label = "levelBadgeScale"
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .graphicsLayer {
                    scaleX = cardScale
                    scaleY = cardScale
                    alpha = cardAlpha
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp),
                shape = RoundedCornerShape(36.dp),
                color = Color.White.copy(alpha = 0.78f),
                shadowElevation = 5.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.96f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFFF6EA), Color(0xFFF8F2FF), Color(0xFFEAF8FF))
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFFEDC2)
                    ) {
                        Text(
                            text = currentChapter.toString(),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            color = Color(0xFF9B6B1B),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "LEVEL $currentChapter",
                        color = Color(0xFF38506A),
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge,
                        letterSpacing = 1.4.sp
                    )
                    if (title.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = title,
                            color = Color(0xFF6B7D90),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }
                }
            }

            VibrantAction(
                onClick = onClick,
                modifier = Modifier
                    .offset(y = 26.dp)
                    .graphicsLayer {
                        scaleX = badgeScale
                        scaleY = badgeScale
                        alpha = cardAlpha
                    }
                    .width(190.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(26.dp),
                brush = Brush.linearGradient(
                    listOf(Color(0xFFFFA860), Color(0xFFFF7C79), Color(0xFFFF5EA2))
                ),
                contentColor = Color.White,
                motion = ActionMotion.BOUNCE_UP
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.6.sp
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(42.dp))
}

@Composable
private fun HistoryFloatingButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    VibrantAction(
        onClick = onClick,
        modifier = modifier.size(58.dp),
        shape = RoundedCornerShape(20.dp),
        brush = Brush.horizontalGradient(listOf(Color(0xFFF5F4FF), Color(0xFFE6F3FF))),
        contentColor = Color(0xFF37516A),
        motion = ActionMotion.SHRINK
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Default.History,
                contentDescription = "History",
                modifier = Modifier.size(24.dp),
                tint = Color(0xFF37516A)
            )
        }
    }
}

@Composable
private fun SoftChromeIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.46f),
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.66f))
    ) {
        Box(
            modifier = Modifier
                .size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = contentDescription, tint = Color(0xFF4B6880))
        }
    }
}

@Composable
private fun TopResourcePill(
    icon: @Composable () -> Unit,
    value: Int,
    tintColor: Color,
    background: Brush,
    onClick: () -> Unit,
    motion: ActionMotion
) {
    VibrantAction(
        onClick = onClick,
        modifier = Modifier
            .width(84.dp)
            .height(42.dp),
        shape = RoundedCornerShape(22.dp),
        brush = background,
        contentColor = tintColor,
        motion = motion
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(Modifier.width(6.dp))
            val displayValue = value.coerceAtMost(999999).toString()
            Text(
                text = displayValue,
                color = tintColor,
                fontWeight = FontWeight.Black,
                fontSize = when {
                    displayValue.length >= 6 -> 10.sp
                    displayValue.length >= 4 -> 11.sp
                    else -> 13.sp
                },
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun PlayInBlockBrand(modifier: Modifier = Modifier) {}

@Composable
private fun CurrentLevelButtonCard(
    currentChapter: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {}

@Composable
private fun HomeResourceMiniButton(
    value: Int,
    badgeColor: Color,
    motion: ActionMotion,
    onClick: () -> Unit,
    backgroundBrush: Brush,
    icon: @Composable () -> Unit
) {}

@Composable
fun SettingsScreen(
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    darkThemeEnabled: Boolean,
    imageSource: ImageSourceMode,
    pexelsConfigured: Boolean,
    onBack: () -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onDarkThemeChanged: (Boolean) -> Unit,
    onImageSourceChanged: (ImageSourceMode) -> Unit,
    onResetProgress: () -> Unit,
    blockMotionPreset: BlockMotionSpeed = BlockMotionSpeed.BALANCED,
    onBlockMotionPresetChanged: (BlockMotionSpeed) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var confirmReset by remember { mutableStateOf(false) }
    var selectedBlockMotion by remember(blockMotionPreset) { mutableStateOf(blockMotionPreset) }

    Column(
        modifier = modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFF2DA), Color(0xFFFFEAF2), Color(0xFFE9F8FF), Color(0xFFF5EEFF))
                )
            )
            .safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        ScreenHeader(title = "Settings", onBack = onBack)
        SettingRow("Sound", soundEnabled, onSoundChanged)
        SettingRow("Haptics", hapticsEnabled, onHapticsChanged)
        SettingRow("Dark Theme", darkThemeEnabled, onDarkThemeChanged)

        Spacer(Modifier.height(24.dp))
        Text("Puzzle Images", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Choose live Pexels photos or artwork bundled with the journey.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        ImageSourceRow(
            title = "Pexels photos",
            subtitle = if (pexelsConfigured) "Online photos are cached per level" else "Online photos are unavailable in this build",
            selected = imageSource == ImageSourceMode.PEXELS,
            icon = { Icon(Icons.Default.Cloud, contentDescription = null) },
            onClick = { onImageSourceChanged(ImageSourceMode.PEXELS) }
        )
        ImageSourceRow(
            title = "Preloaded artwork",
            subtitle = "Works fully offline and uses no network data",
            selected = imageSource == ImageSourceMode.PRELOADED,
            icon = { Icon(Icons.Default.Image, contentDescription = null) },
            onClick = { onImageSourceChanged(ImageSourceMode.PRELOADED) }
        )

        Spacer(Modifier.height(24.dp))
        Text("Block Movement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Choose how quickly puzzle blocks glide into their new positions. Your choice is saved and applied to the puzzle automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        BlockMotionSpeed.values().forEach { preset ->
            MotionPresetRow(
                preset = preset,
                selected = preset == selectedBlockMotion,
                onClick = {
                    selectedBlockMotion = preset
                    onBlockMotionPresetChanged(preset)
                }
            )
        }

        Spacer(Modifier.height(28.dp))
        VibrantAction(
            onClick = { confirmReset = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(18.dp),
            brush = VibrantPinkBrush,
            motion = ActionMotion.SHAKE
        ) {
            Text("Reset Game Progress", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset journey progress?") },
            text = { Text("Completed Adventures, Journey Journal history and the saved puzzle will be cleared. Your coins, Crystals, personal bests, settings and cached photos will stay unchanged.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onResetProgress()
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ImageSourceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    VibrantAction(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(18.dp),
        brush = if (selected) VibrantBlueBrush else Brush.linearGradient(
            listOf(Color(0xFFFFF3CF), Color(0xFFE8F8FF))
        ),
        contentColor = if (selected) Color.White else HomeAccentDeep,
        motion = ActionMotion.SHRINK
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Column(modifier = Modifier.fillMaxWidth(0.78f).padding(horizontal = 12.dp)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) Color.White.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}

private fun BlockMotionSpeed.displayTitle(): String = when (this) {
    BlockMotionSpeed.RELAXED -> "Relaxed"
    BlockMotionSpeed.BALANCED -> "Balanced"
    BlockMotionSpeed.QUICK -> "Quick"
}

private fun BlockMotionSpeed.displaySubtitle(): String = when (this) {
    BlockMotionSpeed.RELAXED -> "Slow and smooth movement"
    BlockMotionSpeed.BALANCED -> "Comfortable default movement"
    BlockMotionSpeed.QUICK -> "Fast and responsive movement"
}

@Composable
private fun MotionPresetRow(
    preset: BlockMotionSpeed,
    selected: Boolean,
    onClick: () -> Unit
) {
    VibrantAction(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(18.dp),
        brush = if (selected) VibrantMintBrush else Brush.linearGradient(
            listOf(Color(0xFFFFE8F0), Color(0xFFEAF7FF))
        ),
        contentColor = if (selected) Color.White else HomeAccentDeep,
        motion = ActionMotion.PULSE
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(preset.displayTitle(), fontWeight = FontWeight.SemiBold)
                Text(
                    "${preset.displaySubtitle()} · ~${preset.durationMillis} ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) Color.White.copy(alpha = 0.88f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

@Composable
fun HowToPlayScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFF2DA), Color(0xFFFFEAF2), Color(0xFFE9F8FF), Color(0xFFF5EEFF))
                )
            )
            .safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        ScreenHeader(title = "How to Play", onBack = onBack)
        Text("1. Press and drag any picture block.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("2. Matching neighbors join into a group. Drag any piece in a group to move the whole shape. Dropping onto another group can split that group.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("3. Blocks glide into their new positions. Keep rearranging until the full image is restored.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("4. You can choose Pexels photos or bundled offline artwork in Settings.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("5. Adventures use tall rectangular grids with more rows than columns. Early puzzles begin around 4×5 and grow up to 8×9, with surprise rectangular grids near the end.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("6. The timer starts automatically as soon as the adventure opens. Pause freezes time while keeping the puzzle visible. Resume continues from the same board.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("7. Coins = completion + speed + efficient moves. The fixed targets depend on grid size. Replays award only improvement over your Adventure best. Coins have no cash value.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("8. Crystal Power: every player receives 2 free Crystals for the whole Journey. Grid Shift spends 1 Crystal to rebuild the current Adventure on a randomly smaller grid, down by 1 or 2 sizes. Fewer cells make larger pieces and an easier puzzle. Restore Grid is always free. When Crystals reach 0, Grid Shift is locked until more are purchased.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("9. The four controls below the puzzle are Timer, Grid Shift, Hint and Restart. The Journey starts with 1 Hint. Each Hint solves one random block/group step and counts as one move. Finish an Adventure within its optimal move target to earn +1 Hint, up to 3 stored Hints.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("10. In Settings, Block Movement lets you choose Relaxed, Balanced, or Quick puzzle motion. Use Relaxed for a slower, smoother glide.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("11. Turn on Guides for numbered pieces. Arrange 1, 2, 3… from left to right, top to bottom. Guides are especially useful for similar-looking sky or blank pieces.", style = MaterialTheme.typography.bodyLarge)

    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VibrantCircleAction(
            onClick = onBack,
            modifier = Modifier.size(44.dp),
            brush = VibrantBlueBrush,
            motion = ActionMotion.SHRINK
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(16.dp))
}

@androidx.compose.ui.tooling.preview.Preview(name = "Journey • compact phone", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun HomeLightPreview() {
    com.hb.puzz.ui.theme.CozyBlocksTheme(darkTheme = false) {
        HomeScreen(crystalBalance = 2, coinBalance = 640, completedCount = 8, hasSavedGame = true,
            currentChapter = 9, currentChapterTitle = "Red Rock Valley",
            currentDifficulty = "Medium · Focus Flow", currentGridDescription = "6×7 Grid",
            journeyComplete = false, onContinue = {}, onStartJourney = {}, onJourneyHistory = {}, onHowToPlay = {}, onSettings = {})
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Journey • night", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun HomeDarkPreview() {
    com.hb.puzz.ui.theme.CozyBlocksTheme(darkTheme = true) {
        HomeScreen(crystalBalance = 2, coinBalance = 640, completedCount = 8, hasSavedGame = true,
            currentChapter = 9, currentChapterTitle = "Red Rock Valley",
            currentDifficulty = "Medium · Focus Flow", currentGridDescription = "6×7 Grid",
            journeyComplete = false, onContinue = {}, onStartJourney = {}, onJourneyHistory = {}, onHowToPlay = {}, onSettings = {})
    }
}
