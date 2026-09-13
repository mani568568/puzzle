package com.hb.puzz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hb.puzz.domain.model.Cell

/**
 * Shows a preview of where a piece would be placed on the board.
 */
@Composable
fun PiecePlacementPreview(
    cells: List<Cell>,
    color: Color,
    isValid: Boolean,
    cellSize: Dp = 40.dp,
    offsetX: Int = 0,
    offsetY: Int = 0
) {
    val shape = RoundedCornerShape(8.dp)
    
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .wrapContentHeight()
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        cells.forEach { cell ->
            BlockPiece(
                color = if (isValid) color else MaterialTheme.colorScheme.error,
                size = cellSize,
                modifier = Modifier.offset(
                    x = cellSize * (cell.x + offsetX),
                    y = cellSize * (cell.y + offsetY)
                )
            )
        }
    }
}

/**
 * Shows a piece in the tray with its cells.
 */
@Composable
fun TrayPiecePreview(
    cells: List<Cell>,
    color: Color,
    cellSize: Dp = 30.dp
) {
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .wrapContentHeight()
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        cells.forEach { cell ->
            BlockPiece(
                color = color,
                size = cellSize,
                modifier = Modifier.offset(
                    x = cellSize * cell.x,
                    y = cellSize * cell.y
                )
            )
        }
    }
}
