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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Image
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

@Composable
fun HomeScreen(
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
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Cozy Picture Journey",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Restore each picture to reveal the next chapter.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(28.dp))

        Card(modifier = Modifier.fillMaxWidth(0.84f)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (journeyComplete) "Journey Complete" else "Current Chapter",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (journeyComplete) "All pictures restored ✨" else "Chapter $currentChapter · $currentChapterTitle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!journeyComplete) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "$currentGridDescription  •  $currentDifficulty",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = if (hasSavedGame) onContinue else onStartJourney,
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
            shape = CircleShape
        ) {
            Text(
                when {
                    hasSavedGame -> "Continue Journey"
                    journeyComplete -> "Replay Final Chapter"
                    currentChapter == 1 -> "Begin Journey"
                    else -> "Start Chapter $currentChapter"
                }
            )
        }

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
            "Choose live Pexels photos or artwork bundled with the journey.",
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
            title = { Text("Reset journey progress?") },
            text = { Text("Completed chapters and the saved puzzle will be cleared. Your settings and cached Pexels photos will stay unchanged.") },
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
        Text("5. Chapters 1–5 use 4×4 grids. Later chapters grow through 5×5, 6×6, 7×7 and 8×8, with surprise-grid chapters near the end.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Text("6. Difficulty grows from Easy · Cozy Start to Medium · Focus Flow and Hard · Master Quest.", style = MaterialTheme.typography.bodyLarge)
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
