package com.hb.puzz.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
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
import com.hb.puzz.domain.PuzzleLevel

@Composable
fun HomeScreen(
    hasSavedGame: Boolean,
    onContinue: () -> Unit,
    onStartNewGame: () -> Unit,
    onHowToPlay: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Cozy Picture Blocks",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        Text("Drag picture blocks into place to restore the artwork.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(40.dp))

        if (hasSavedGame) {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
                shape = CircleShape
            ) { Text("Continue") }
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = onStartNewGame,
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
            shape = CircleShape
        ) { Text("Choose Level") }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onHowToPlay,
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
            shape = CircleShape
        ) { Text("How to Play") }

        Spacer(Modifier.height(20.dp))
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }
    }
}

@Composable
fun LevelSelectionScreen(
    highestUnlockedLevel: Int,
    completedLevels: Set<Int>,
    onBack: () -> Unit,
    onLevelSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)
    ) {
        ScreenHeader(title = "Choose a Level", onBack = onBack)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(PuzzleLevel.ALL_LEVELS, key = { it.id }) { level ->
                val unlocked = level.id <= highestUnlockedLevel
                val completed = level.id in completedLevels
                Card(
                    onClick = { if (unlocked) onLevelSelected(level.id) },
                    enabled = unlocked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Level ${level.id} · ${level.title}", fontWeight = FontWeight.SemiBold)
                            Text(
                                "${level.gridSize} × ${level.gridSize} · ${level.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        when {
                            completed -> Icon(Icons.Default.CheckCircle, contentDescription = "Completed")
                            !unlocked -> Icon(Icons.Default.Lock, contentDescription = "Locked")
                        }
                    }
                }
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
    modifier: Modifier = Modifier
) {
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)
    ) {
        ScreenHeader(title = "Settings", onBack = onBack)
        SettingRow("Sound", soundEnabled, onSoundChanged)
        SettingRow("Haptics", hapticsEnabled, onHapticsChanged)
        SettingRow("Dark Theme", darkThemeEnabled, onDarkThemeChanged)

        Spacer(Modifier.height(24.dp))
        Text("Puzzle Images", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Choose live Pexels photos or the artwork bundled with the app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        ImageSourceRow(
            title = "Pexels photos",
            subtitle = if (pexelsConfigured) "Online photos are cached per level" else "API key not configured — offline artwork will be used as fallback",
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
            title = { Text("Reset progress?") },
            text = { Text("Completed levels and the saved puzzle will be cleared. Your settings and cached Pexels photos will stay unchanged.") },
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
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)
    ) {
        ScreenHeader(title = "How to Play", onBack = onBack)
        Text("1. Press and drag any picture block.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("2. Drop it over another block to swap their positions.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("3. Blocks glide into their new positions. Keep rearranging until the full image is restored.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("4. You can choose Pexels photos or bundled offline artwork in Settings.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("5. Completing a level unlocks the next one. Later levels use larger 4×4 and 5×5 grids.", style = MaterialTheme.typography.bodyLarge)
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
