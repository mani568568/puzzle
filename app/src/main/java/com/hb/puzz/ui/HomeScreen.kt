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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    listOf(Color(0xFFFFF1D6), Color(0xFFFFE6F0), Color(0xFFE7F7FF), Color(0xFFF4F0FF))
                )
            )
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(26.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                PlayInBlockBrand(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                )
            }

            Spacer(Modifier.height(72.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CurrentLevelButtonCard(
                    currentChapter = currentChapter,
                    onClick = primaryAction,
                    modifier = Modifier.weight(1f)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HomeResourceMiniButton(
                        value = crystalBalance,
                        badgeColor = Color(0xFF3FCBEA),
                        motion = ActionMotion.PULSE,
                        onClick = onHowToPlay,
                        icon = { CrystalIcon(24.dp) }
                    )
                    HomeResourceMiniButton(
                        value = coinBalance,
                        badgeColor = Color(0xFFFFB52E),
                        motion = ActionMotion.BOUNCE_UP,
                        onClick = onHowToPlay,
                        icon = { GoldCoinIcon(Modifier.size(24.dp)) }
                    )
                }
            }

        }

        VibrantCircleAction(
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 2.dp, end = 18.dp)
                .size(48.dp),
            brush = VibrantPinkBrush,
            motion = ActionMotion.TILT
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
        }

        VibrantAction(
            onClick = onJourneyHistory,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(58.dp),
            shape = RoundedCornerShape(20.dp),
            brush = VibrantBlueBrush,
            motion = ActionMotion.BOUNCE_UP
        ) {
            Icon(Icons.Default.History, contentDescription = "Adventure History", tint = Color.White)
        }
    }
}


@Composable
private fun PlayInBlockBrand(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(TitlePanelTop, TitlePanelBottom)
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 12.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "PLAY",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = TitleLight,
                fontSize = 58.sp,
                lineHeight = 60.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
            Text(
                text = "IN",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = TitleWarm,
                fontSize = 36.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                letterSpacing = 9.sp
            )
            Text(
                text = "BLOCK",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = TitleLight,
                fontSize = 48.sp,
                lineHeight = 50.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.2.sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun CurrentLevelButtonCard(
    currentChapter: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    VibrantAction(
        onClick = onClick,
        modifier = modifier
            .height(84.dp),
        shape = RoundedCornerShape(28.dp),
        brush = VibrantOrangeBrush,
        motion = ActionMotion.BOUNCE_UP
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "LEVEL $currentChapter",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun HomeResourceMiniButton(
    value: Int,
    badgeColor: Color,
    motion: ActionMotion,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .widthIn(min = 54.dp)
            .heightIn(min = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        VibrantAction(
            onClick = onClick,
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            brush = Brush.linearGradient(
                listOf(Color(0x28FFFFFF), Color(0x10FFFFFF))
            ),
            motion = motion
        ) {
            icon()
        }
        val text = value.coerceAtMost(999999).toString()
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .widthIn(min = 20.dp, max = 52.dp)
                .heightIn(min = 18.dp),
            shape = CircleShape,
            color = badgeColor,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.88f))
        ) {
            Box(
                modifier = Modifier.padding(horizontal = if (text.length >= 5) 4.dp else 6.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = Color(0xFF352400),
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
