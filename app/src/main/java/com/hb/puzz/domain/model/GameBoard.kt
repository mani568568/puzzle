package com.hb.puzz.domain.model

/** Immutable state of the 8x8 block-placement board. */
data class GameBoard(
    val width: Int = 8,
    val height: Int = 8,
    val cells: Map<Cell, PieceColor> = emptyMap()
) {
    companion object {
        fun createEmptyBoard(width: Int = 8, height: Int = 8): GameBoard =
            GameBoard(width = width, height = height)
    }

    fun isFull(): Boolean = cells.size >= width * height

    /** True only when every translated piece cell is inside the board and empty. */
    fun canPlacePiece(piece: Piece, x: Int, y: Int): Boolean {
        if (piece.cells.isEmpty()) return false

        return piece.withOffset(x, y).all { cell ->
            cell.x in 0 until width &&
                cell.y in 0 until height &&
                cell !in cells
        }
    }

    /**
     * Places pieces whose cell coordinates are already board-relative.
     * This helper is mainly useful for tests/setup; gameplay placement uses GameEngine.
     */
    fun placePiece(pieces: List<Piece>): GameBoard {
        val updated = cells.toMutableMap()
        pieces.forEach { piece ->
            piece.cells.forEach { cell ->
                if (cell.x in 0 until width && cell.y in 0 until height) {
                    updated[cell] = piece.color
                }
            }
        }
        return copy(cells = updated)
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
