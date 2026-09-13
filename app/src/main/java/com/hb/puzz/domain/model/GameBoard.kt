package com.hb.puzz.domain.model

/**
 * Represents the game board state.
 */
data class GameBoard(
    val width: Int = 8,
    val height: Int = 8,
    val cells: Map<Cell, PieceColor> = emptyMap()
) {
    /**
     * Creates a copy of this board with the specified cell set to the given color.
     */
    fun placePiece(pieces: List<Piece>): GameBoard {
        // Combine all pieces and create new board
        val newCells = mutableMapOf<Cell, PieceColor>()
        newCells.putAll(cells)
        
        pieces.forEach { piece ->
            piece.cells.forEach { cell -> newCells[cell] = piece.color }
        }

        return copy(cells = newCells)
    }

    /**
     * Checks if a piece can be placed at the given position without overlap or going out of bounds.
     */
    fun canPlacePiece(piece: Piece, offsetX: Int, offsetY: Int): Boolean {
        val placedCells = piece.withOffset(offsetX, offsetY)
        
        return placedCells.isNotEmpty() && placedCells.all { cell ->
            // Check bounds
            if (cell.x < 0 || cell.x >= width || cell.y < 0 || cell.y >= height) {
                return@all false
            }
            
            // Check if cell is already occupied
            if (cells.containsKey(cell)) {
                return@all false
            }
            
            true
        }
    }

    /**
     * Checks if the board is full.
     */
    fun isFull(): Boolean = cells.size == width * height

    /**
     * Returns a list of completed rows and columns after placing pieces.
     */
    fun getCompletedLines(): Pair<List<Int>, List<Int>> {
        val rowsToClear = mutableListOf<Int>()
        val colsToClear = mutableListOf<Int>()

        // Check rows
        for (row in 0 until height) {
            if ((0 until width).all { col -> cells.containsKey(Cell(col, row)) }) {
                rowsToClear.add(row)
            }
        }

        // Check columns
        for (col in 0 until width) {
            if ((0 until height).all { row -> cells.containsKey(Cell(col, row)) }) {
                colsToClear.add(col)
            }
        }

        return rowsToClear to colsToClear
    }

    /**
     * Clears completed lines and returns a new board.
     */
    fun clearLines(rows: List<Int>, cols: List<Int>): GameBoard {
        val newCells = cells.filterNot { (cell) ->
            // Remove if in any row or column to clear
            rows.contains(cell.y) || cols.contains(cell.x)
        }
        
        return copy(cells = newCells)
    }

    /**
     * Returns the number of filled cells.
     */
    fun getFilledCellCount(): Int = cells.size

    companion object {
        fun createEmptyBoard(width: Int = 8, height: Int = 8): GameBoard {
            return GameBoard(width, height, emptyMap())
        }
    }
}
