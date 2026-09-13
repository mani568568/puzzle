package com.hb.puzz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hb.puzz.domain.model.PieceColor

/**
 * A single cell in the game board.
 */
@Composable
fun BoardCell(
    modifier: Modifier = Modifier,
    color: Color? = null,
    showPreview: Boolean = false,
    isValidPlacement: Boolean = true,
    previewColor: Color? = null,
    cellSize: Dp = 40.dp
) {
    val cellShape = RoundedCornerShape(6.dp)
    
    Box(
        modifier = modifier
            .size(cellSize)
            .clip(cellShape)
            .background(if (color != null) color else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                shape = cellShape
            )
    ) {
        // Show placement preview if applicable
        if (showPreview && isValidPlacement) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(cellShape)
                    .background(previewColor ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            )
        }
    }
}

/**
 * Shows a block with rounded corners.
 */
@Composable
fun BlockPiece(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    alpha: Float = 1.0f
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color.copy(alpha = alpha))
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 2.dp,
                color = color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
    )
}
