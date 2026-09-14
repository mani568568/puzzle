package com.hb.puzz.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hb.puzz.domain.PuzzleEngine

@Composable
fun PuzzleBoard(
    engine: PuzzleEngine,
    onTileSwapped: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    tileSize: Dp = 60.dp
) {
    val gridSize = engine.gridSize
    
    var selectedTile by remember { mutableStateOf<Int?>(null) }
    
    Box(
        modifier = modifier
            .size(with(LocalDensity.current) { (gridSize * tileSize).roundToPx().dp })
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.background)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cellWidth = size.width / gridSize
            val cellHeight = size.height / gridSize
            
            for (pos in 0 until engine.getTotalTiles()) {
                val row = pos / gridSize
                val col = pos % gridSize
                
                drawRect(
                    color = ComposeColor(0xFFF5F1E8),
                    topLeft = Offset((col * cellWidth).toFloat(), (row * cellHeight).toFloat()),
                    size = Size(cellWidth, cellHeight)
                )
                
                drawRect(
                    color = MaterialTheme.colorScheme.primary,
                    topLeft = androidx.compose.ui.geometry.Offset((col * cellWidth + 2.dp.toPx()).toFloat(), (row * cellHeight + 2.dp.toPx()).toFloat()),
                    size = Size(cellWidth - 4.dp.toPx(), cellHeight - 4.dp.toPx())
                )
            }
        }
        
        for (pos in 0 until engine.getTotalTiles()) {
            val row = pos / gridSize
            val col = pos % gridSize
            
            Box(
                modifier = size(tileSize)
                    .offset { IntOffset(col * tileSize.roundToPx(), row * tileSize.roundToPx()) }
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                    .border(2.dp, MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .pointerInput(pos) {
                        detectTapGestures { offset ->
                            if (selectedTile == null) {
                                selectedTile = pos
                            } else if (selectedTile != pos) {
                                onTileSwapped(selectedTile!!, pos)
                                selectedTile = null
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("${engine.getTileAt(pos)}")
            }
        }
    }
}
