package com.hb.puzz.domain.model

/** A single board coordinate. */
data class Cell(val x: Int, val y: Int)

/** A placeable block piece made from one or more cells. */
data class Piece(
    val id: Int,
    val cells: List<Cell>,
    val color: PieceColor
) {
    /** Returns this piece's cells translated by the requested board offset. */
    fun withOffset(offsetX: Int, offsetY: Int): List<Cell> =
        cells.map { cell -> Cell(cell.x + offsetX, cell.y + offsetY) }

    /**
     * Bounding size measured from the piece origin (0,0).
     * Example: cells x=0..3, y=0 produces 4 x 1.
     */
    val bounds: Pair<Int, Int>
        get() {
            if (cells.isEmpty()) return 0 to 0
            return (cells.maxOf { it.x } + 1) to (cells.maxOf { it.y } + 1)
        }
}
