package com.hb.puzz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Main activity for Cozy Picture Blocks.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                GameScreen()
            }
        }
    }
}

@Composable
fun GameScreen() {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5F1E8)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Cozy Picture Blocks", style = MaterialTheme.typography.displayLarge)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { /* Navigate to game */ },
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
        ) {
            Text("Start Game")
        }
    }
}
