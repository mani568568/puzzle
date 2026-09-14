package com.hb.puzz.domain.model

/** Immutable state of the block-placement board. */
data class GameBoard(
    val width: Int = 8,
    val height: Int = 8,
    val cells: Map<Cell, PieceColor> = emptyMap()
) {
    companion object {
        fun createEmptyBoard(width: Int = 8, height: Int = 8): GameBoard {
            require(width > 0) { "Board width must be greater than 0" }
            require(height > 0) { "Board height must be greater than 0" }
            return GameBoard(width = width, height = height)
        }
    }

    fun isFull(): Boolean = cells.size >= width * height

    /** True only when every translated piece cell is inside the board and empty. */
    fun canPlacePiece(piece: Piece, x: Int, y: Int): Boolean {
        if (piece.cells.isEmpty()) return false
        if (piece.cells.distinct().size != piece.cells.size) return false

        return piece.withOffset(x, y).all { cell ->
            cell.x in 0 until width &&
                cell.y in 0 until height &&
                cell !in cells
        }
    }

    /**
     * Places one piece at the requested board position.
     * Returns null when the move is invalid so callers cannot accidentally
     * overwrite an occupied cell or write outside the board.
     */
    fun placePiece(piece: Piece, x: Int, y: Int): GameBoard? {
        if (!canPlacePiece(piece, x, y)) return null

        val updated = cells.toMutableMap()
        piece.withOffset(x, y).forEach { cell ->
            updated[cell] = piece.color
        }
        return copy(cells = updated)
    }

    /**
     * Places pieces whose cell coordinates are already board-relative.
     * This helper is useful for tests/setup. Invalid or overlapping pieces
     * are ignored rather than overwriting existing board cells.
     */
    fun placePiece(pieces: List<Piece>): GameBoard {
        var result = this

        pieces.forEach { piece ->
            val validCells = piece.cells.isNotEmpty() &&
                piece.cells.distinct().size == piece.cells.size &&
                piece.cells.all { cell ->
                    cell.x in 0 until result.width &&
                        cell.y in 0 until result.height &&
                        cell !in result.cells
                }

            if (validCells) {
                val updated = result.cells.toMutableMap()
                piece.cells.forEach { cell -> updated[cell] = piece.color }
                result = result.copy(cells = updated)
            }
        }

        return result
    }

    /** Returns completed row indexes and completed column indexes. */
    fun getCompletedLines(): Pair<List<Int>, List<Int>> {
        val completedRows = (0 until height).filter { y ->
            (0 until width).all { x -> Cell(x, y) in cells }
        }
        val completedColumns = (0 until width).filter { x ->
            (0 until height).all { y -> Cell(x, y) in cells }
        }
        return completedRows to completedColumns
    }

    /** Removes every cell belonging to any completed row or column. */
    fun clearLines(rows: Collection<Int>, columns: Collection<Int>): GameBoard {
        if (rows.isEmpty() && columns.isEmpty()) return this

        val remaining = cells.filterKeys { cell ->
            cell.y !in rows && cell.x !in columns
        }
        return copy(cells = remaining)
    }
}
