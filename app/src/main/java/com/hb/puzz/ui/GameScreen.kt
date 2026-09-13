package com.hb.puzz.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hb.puzz.domain.model.Piece
import com.hb.puzz.ui.components.BlockPiece
import com.hb.puzz.ui.components.GameBoard
import com.hb.puzz.ui.components.PlacementPreview
import kotlin.math.floor

@Composable
fun GameScreen(
    state: GameUiState,
    onPiecePlaced: (Piece, Int, Int) -> Boolean,
    onPaused: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf<Piece?>(null) }
    var boardBounds by remember { mutableStateOf<Rect?>(null) }
    var dragPoint by remember { mutableStateOf<Offset?>(null) }
    var message by remember { mutableStateOf("Select a piece, then tap the board. You can also drag a piece.") }
    fun target(point: Offset): IntOffset? {
        val bounds = boardBounds ?: return null
        if (bounds.width <= 0 || bounds.height <= 0 || !bounds.contains(point)) return null
        return IntOffset(floor((point.x - bounds.left) / (bounds.width / state.board.width)).toInt(),
            floor((point.y - bounds.top) / (bounds.height / state.board.height)).toInt())
    }
    fun place(piece: Piece, x: Int, y: Int) {
        if (onPiecePlaced(piece, x, y)) {
            selected = null
            message = "Piece placed. Choose your next piece."
        } else message = "That piece does not fit there. Try another cell."
    }
    Column(modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Mosaic Blocks", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onPaused) { Icon(Icons.Default.Pause, contentDescription = "Pause") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Score: ${state.score}")
            Text("Best: ${state.bestScore}")
        }
        if (state.saveError) Text("Unable to save progress on this device.", color = MaterialTheme.colorScheme.error)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val size = minOf(maxWidth / state.board.width, maxHeight / state.board.height, 44.dp)
            Box {
                GameBoard(state.board, onCellClicked = { x, y -> selected?.let { place(it, x, y) } },
                    cellSize = size, modifier = Modifier.onGloballyPositioned { boardBounds = it.boundsInRoot() })
                val piece = selected
                val position = dragPoint?.let { target(it) }
                if (piece != null && position != null) {
                    PlacementPreview(position, piece.cells, Color(android.graphics.Color.parseColor(piece.color.hex)),
                        state.board.canPlacePiece(piece, position.x, position.y), cellSize = size)
                }
            }
        }
        Text(message, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.pieces.forEach { piece ->
                key(piece) {
                    var trayOrigin by remember { mutableStateOf(Offset.Zero) }
                    // Keep event callbacks current without cancelling an active gesture on recomposition.
                    val move by rememberUpdatedState<(Offset) -> Unit>({ point -> selected = piece; dragPoint = point })
                    val drop by rememberUpdatedState<() -> Unit>({
                        val position = dragPoint?.let { target(it) }
                        if (position != null) place(piece, position.x, position.y)
                        dragPoint = null
                    })
                    BoxWithConstraints(Modifier.weight(1f)) {
                        val pieceCellSize = minOf(24.dp, (maxWidth - 16.dp) / piece.bounds.first)
                        Box(Modifier.fillMaxWidth().height(140.dp)
                            .border(if (selected == piece) 2.dp else 1.dp,
                                if (selected == piece) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .onGloballyPositioned { trayOrigin = it.boundsInRoot().topLeft }
                            .clickable { selected = piece; message = "Tap the board at the piece's top-left corner." }
                            .pointerInput(piece) {
                                var point = Offset.Zero
                                detectDragGestures(
                                    onDragStart = { local -> point = trayOrigin + local; move(point) },
                                    onDrag = { change, amount -> change.consume(); point += amount; move(point) },
                                    onDragEnd = { drop() },
                                    onDragCancel = { dragPoint = null }
                                )
                            }, contentAlignment = Alignment.Center) {
                            PieceTrayItem(piece, pieceCellSize)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieceTrayItem(piece: Piece, cellSize: Dp = 24.dp) {
    Box(Modifier.size(cellSize * piece.bounds.first, cellSize * piece.bounds.second)) {
        piece.cells.forEach { cell ->
            BlockPiece(Color(android.graphics.Color.parseColor(piece.color.hex)), size = cellSize,
                modifier = Modifier.offset(cellSize * cell.x, cellSize * cell.y))
        }
    }
}
