package com.hb.puzz.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hb.puzz.data.images.ImageSourceMode

@Composable
fun HomeScreen(
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
    onHowToPlay: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("PICTURE JOURNEY", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            CoinPill(coinBalance, onClick = onHowToPlay)
        }
        Text(if (journeyComplete) "A beautiful journey." else "A little focus.\nA beautiful picture.",
            modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold)
        Text("$completedCount of 20 discoveries completed", modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Extension, null, Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text(if (journeyComplete) "Journey complete" else "Discovery $currentChapter",
                    style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold)
                Text(currentGridDescription, style = MaterialTheme.typography.bodyMedium)
                Text(currentDifficulty, style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(onClick = if (hasSavedGame) onContinue else onStartJourney,
            modifier = Modifier.fillMaxWidth().height(56.dp), shape = CircleShape) {
            Text(if (hasSavedGame) "Continue your picture" else if (journeyComplete) "Replay final discovery" else "Let’s play")
        }
        Text("Complete pictures. Earn coins. Find your flow.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = onHowToPlay) { Text("How to play") }
            TextButton(onClick = onSettings) { Text("Settings") }
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
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
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
            subtitle = if (pexelsConfigured) "Online photos are cached per discovery" else "Online photos are unavailable in this build",
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
            text = { Text("Completed discoveries and the saved puzzle will be cleared. Your coins, personal bests, settings and cached photos will stay unchanged.") },
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
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
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
        Text("5. Discoveries 1–5 use 4×4 grids. Later discoveries grow through 5×5, 6×6, 7×7 and 8×8, with surprise grids near the end.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("6. Start begins the play clock. Pause hides the board and freezes time. Resume when you’re ready. Restart begins a fresh attempt after confirmation.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("7. Coins = completion + speed + efficient moves. The fixed targets depend on grid size. Replays award only improvement over your best for that discovery. Coins have no cash value.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("8. A gold line traces newly merged shapes as they gently lift and settle. Watch the connection bar grow as you complete each discovery.", style = MaterialTheme.typography.bodyLarge)
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
        HomeScreen(coinBalance = 640, completedCount = 8, hasSavedGame = true,
            currentChapter = 9, currentChapterTitle = "Red Rock Valley",
            currentDifficulty = "Medium · Focus Flow", currentGridDescription = "6×6 Grid",
            journeyComplete = false, onContinue = {}, onStartJourney = {}, onHowToPlay = {}, onSettings = {})
    }
}
@androidx.compose.ui.tooling.preview.Preview(name = "Journey • night", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun HomeDarkPreview() {
    com.hb.puzz.ui.theme.CozyBlocksTheme(darkTheme = true) {
        HomeScreen(coinBalance = 640, completedCount = 8, hasSavedGame = true,
            currentChapter = 9, currentChapterTitle = "Red Rock Valley",
            currentDifficulty = "Medium · Focus Flow", currentGridDescription = "6×6 Grid",
            journeyComplete = false, onContinue = {}, onStartJourney = {}, onHowToPlay = {}, onSettings = {})
    }
}
