package com.hb.puzz.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The home screen with game options.
 */
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
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Mosaic Blocks",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        
        if (hasSavedGame) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(56.dp),
                shape = CircleShape
            ) {
                Text("Continue", style = MaterialTheme.typography.bodyLarge)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Button(
            onClick = onStartNewGame,
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(56.dp),
            shape = CircleShape
        ) {
            Text("New Game", style = MaterialTheme.typography.bodyLarge)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onHowToPlay,
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(56.dp),
            shape = CircleShape
        ) {
            Text("How to Play", style = MaterialTheme.typography.bodyLarge)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.End)) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings"
            )
        }
    }
}

/**
 * Settings screen.
 */
@Composable
fun SettingsScreen(
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    darkThemeEnabled: Boolean,
    onSoundChanged: (Boolean) -> Unit,
    onHapticsChanged: (Boolean) -> Unit,
    onDarkThemeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Sound toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Sound", style = MaterialTheme.typography.bodyMedium)
            
            Switch(
                checked = soundEnabled,
                onCheckedChange = onSoundChanged
            )
        }
        
        // Haptics toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Haptics", style = MaterialTheme.typography.bodyMedium)
            
            Switch(
                checked = hapticsEnabled,
                onCheckedChange = onHapticsChanged
            )
        }
        
        // Dark theme toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Dark Theme", style = MaterialTheme.typography.bodyMedium)
            
            Switch(
                checked = darkThemeEnabled,
                onCheckedChange = onDarkThemeChanged
            )
        }
    }
}

/**
 * How to Play screen.
 */
@Composable
fun HowToPlayScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "How to Play",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = "1. Place Pieces",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text("Drag a piece from the tray onto the board, or tap a piece then tap its top-left placement cell.")
            
            Text(
                text = "2. Complete Lines",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            Text("Fill entire rows or columns to clear them and score points.")
            
            Text(
                text = "3. Scoring",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            Text("• 1 point per placed cell")
            Text("• 10 points per completed line")
            Text("• Bonus: 5 × L × (L-1) for clearing L lines at once")
            
            Text(
                text = "4. Game Over",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            Text("When no pieces can be placed, the game ends.")
        }
    }
}

/**
 * Pause screen.
 */
@Composable
fun PauseScreen(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Paused",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onResume,
            modifier = Modifier.fillMaxWidth(0.75f).height(56.dp)
        ) {
            Text("Resume", style = MaterialTheme.typography.bodyLarge)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth(0.75f).height(56.dp)
        ) {
            Text("Restart", style = MaterialTheme.typography.bodyLarge)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = onHome,
            modifier = Modifier.fillMaxWidth(0.75f).height(56.dp)
        ) {
            Text("Home", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * Game over screen.
 */
@Composable
fun GameOverScreen(
    score: Int,
    bestScore: Int,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Game Over",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Score", style = MaterialTheme.typography.bodyMedium)
            Text(score.toString(), style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold))
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Best Score", style = MaterialTheme.typography.bodyMedium)
            Text(bestScore.toString(), style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold))
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Button(
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth(0.75f).height(56.dp)
        ) {
            Text("Play Again", style = MaterialTheme.typography.bodyLarge)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = onHome,
            modifier = Modifier.fillMaxWidth(0.75f).height(56.dp)
        ) {
            Text("Home", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
