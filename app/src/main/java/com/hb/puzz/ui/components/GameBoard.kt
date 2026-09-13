package com.hb.puzz.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hb.puzz.domain.model.Cell
import com.hb.puzz.domain.model.GameBoard as BoardState

@Composable
fun GameBoard(
    board: BoardState,
    onCellClicked: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    cellSize: Dp = 40.dp
) {
    Column(modifier.size(cellSize * board.width, cellSize * board.height)) {
        repeat(board.height) { y ->
            Row {
                repeat(board.width) { x ->
                    val color = board.cells[Cell(x, y)]?.let { Color(android.graphics.Color.parseColor(it.hex)) }
                    BoardCell(color = color, cellSize = cellSize,
                        modifier = Modifier.clickable { onCellClicked(x, y) })
                }
            }
        }
    }
}

@Composable
fun PlacementPreview(
    position: IntOffset,
    cells: List<Cell>,
    color: Color,
    isValid: Boolean,
    modifier: Modifier = Modifier,
    cellSize: Dp = 40.dp
) {
    Box(modifier.offset(cellSize * position.x, cellSize * position.y)) {
        cells.forEach { cell ->
            BlockPiece(color = if (isValid) color else MaterialTheme.colorScheme.error,
                size = cellSize, alpha = 0.6f,
                modifier = Modifier.offset(cellSize * cell.x, cellSize * cell.y))
        }
    }
}
