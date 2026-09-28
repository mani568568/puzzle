package com.hb.puzz.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hb.puzz.domain.PuzzleLevel
import com.hb.puzz.ui.images.LevelArtworkThumbnail

@Composable
fun LevelSelectionScreen(
    highestUnlocked: Int,
    completedLevels: Set<Int>,
    onBack: () -> Unit,
    onSelectLevel: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFF4DF), Color(0xFFEAF8FF), Color(0xFFF3EEFF))
                )
            )
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VibrantCircleAction(
                onClick = onBack,
                modifier = Modifier.size(48.dp),
                brush = VibrantBlueBrush,
                motion = ActionMotion.SHRINK
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Choose an Adventure",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF243C55)
                )
                Text(
                    text = "${PuzzleLevel.maxLevelId} picture puzzles",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF65788B)
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(PuzzleLevel.ALL_LEVELS, key = { it.id }) { level ->
                val unlocked = level.id <= highestUnlocked
                val completed = level.id in completedLevels
                LevelGalleryCard(
                    level = level,
                    unlocked = unlocked,
                    completed = completed,
                    onClick = { if (unlocked) onSelectLevel(level.id) }
                )
            }
        }
    }
}

@Composable
private fun LevelGalleryCard(
    level: PuzzleLevel,
    unlocked: Boolean,
    completed: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) Color.White.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.92f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                LevelArtworkThumbnail(
                    levelId = level.id,
                    contentDescription = "Level ${level.id} artwork",
                    modifier = Modifier.fillMaxSize(),
                    alpha = if (unlocked) 1f else 0.42f
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.88f)
                ) {
                    Text(
                        text = level.id.toString().padStart(3, '0'),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF2F4D65)
                    )
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.90f)
                ) {
                    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                completed -> Icons.Default.Check
                                unlocked -> Icons.Default.PlayArrow
                                else -> Icons.Default.Lock
                            },
                            contentDescription = when {
                                completed -> "Completed"
                                unlocked -> "Unlocked"
                                else -> "Locked"
                            },
                            tint = when {
                                completed -> Color(0xFF2D9E67)
                                unlocked -> Color(0xFF4967C8)
                                else -> Color(0xFF7C858F)
                            }
                        )
                    }
                }
            }

            Text(
                text = level.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color(0xFF31465C),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = level.gridDescription,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6E7D8D),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            VibrantAction(
                onClick = onClick,
                enabled = unlocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(18.dp),
                brush = if (completed) VibrantMintBrush else VibrantBlueBrush,
                motion = ActionMotion.BOUNCE_UP
            ) {
                Text(
                    text = when {
                        !unlocked -> "LOCKED"
                        completed -> "REPLAY"
                        else -> "PLAY"
                    },
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}
