package com.hb.puzz.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hb.puzz.data.images.ImageSourceMode

private val HomeCream = Color(0xFFFFF7EA)
private val HomePanel = Color(0xFFFFFCF4)
private val HomeAccent = Color(0xFFB86A3B)
private val HomeAccentSoft = Color(0xFFF4E1C9)
private val HomeAccentDeep = Color(0xFF8D4F23)

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
            .background(HomeCream)
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
            Box(modifier = Modifier.fillMaxWidth()) {
                PlayInBlockBrand(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 42.dp)
                )
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(HomePanel, CircleShape)
                        .size(46.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = HomeAccent)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CrystalPill(crystalBalance, onClick = onHowToPlay)
                Spacer(Modifier.width(10.dp))
                CoinPill(coinBalance, onClick = onHowToPlay)
            }

            Spacer(Modifier.height(74.dp))

            CurrentLevelButtonCard(
                currentChapter = currentChapter,
                onClick = primaryAction
            )

        }

        FloatingActionButton(
            onClick = onJourneyHistory,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Default.History, contentDescription = "Journey History")
        }
    }
}


@Composable
private fun PlayInBlockBrand(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = "PLAY",
            modifier = Modifier.fillMaxWidth().graphicsLayer { shadowElevation = 4f },
            textAlign = TextAlign.Center,
            color = HomeAccentDeep,
            fontSize = 46.sp,
            lineHeight = 48.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp
        )
        Text(
            text = "IN",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = HomeAccent,
            fontSize = 30.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 8.sp
        )
        Text(
            text = "BLOCK",
            modifier = Modifier.fillMaxWidth().graphicsLayer { shadowElevation = 4f },
            textAlign = TextAlign.Center,
            color = HomeAccentDeep,
            fontSize = 46.sp,
            lineHeight = 48.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun CurrentLevelButtonCard(
    currentChapter: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = HomeAccentSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                color = HomeAccentDeep
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = HomeAccentDeep,
                modifier = Modifier.size(28.dp)
            )
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
    modifier: Modifier = Modifier
) {
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().background(HomeCream)
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

        Spacer(Modifier.height(28.dp))
        OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Reset Game Progress")
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
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Column(modifier = Modifier.fillMaxWidth(0.78f).padding(horizontal = 12.dp)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
fun HowToPlayScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(HomeCream)
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
        Text("5. Adventures begin with 4×4 grids, then grow through 5×5, 6×6, 7×7 and 8×8, with surprise grids near the end.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("6. The timer starts automatically as soon as the adventure opens. Pause freezes time while keeping the puzzle visible. Resume continues from the same board.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("7. Coins = completion + speed + efficient moves. The fixed targets depend on grid size. Replays award only improvement over your Adventure best. Coins have no cash value.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("8. Crystal Power: every player receives 2 free Crystals for the whole Journey. Grid Shift spends 1 Crystal to rebuild the current Adventure on a randomly smaller grid, down by 1 or 2 sizes. Fewer cells make larger pieces and an easier puzzle. Restore Grid is always free. When Crystals reach 0, Grid Shift is locked until more are purchased.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("9. The four controls below the puzzle are Timer, Grid Shift, Hint and Restart. The Journey starts with 1 Hint. Each Hint solves one random block/group step and counts as one move. Finish an Adventure within its optimal move target to earn +1 Hint, up to 3 stored Hints.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("10. Turn on Guides for numbered pieces. Arrange 1, 2, 3… from left to right, top to bottom. Guides are especially useful for similar-looking sky or blank pieces.", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
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
            currentDifficulty = "Medium · Focus Flow", currentGridDescription = "6×6 Grid",
            journeyComplete = false, onContinue = {}, onStartJourney = {}, onJourneyHistory = {}, onHowToPlay = {}, onSettings = {})
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Journey • night", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun HomeDarkPreview() {
    com.hb.puzz.ui.theme.CozyBlocksTheme(darkTheme = true) {
        HomeScreen(crystalBalance = 2, coinBalance = 640, completedCount = 8, hasSavedGame = true,
            currentChapter = 9, currentChapterTitle = "Red Rock Valley",
            currentDifficulty = "Medium · Focus Flow", currentGridDescription = "6×6 Grid",
            journeyComplete = false, onContinue = {}, onStartJourney = {}, onJourneyHistory = {}, onHowToPlay = {}, onSettings = {})
    }
}
