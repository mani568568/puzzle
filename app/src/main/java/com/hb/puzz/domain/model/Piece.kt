package com.hb.puzz.domain.model

/**
 * Represents a game piece with its shape and color.
 */
data class Piece(
    val id: Int,
    val cells: List<Cell>,
    val color: PieceColor
) {
    /**
     * The bounding box size of this piece (width, height).
     */
    val bounds: Pair<Int, Int> by lazy {
        val maxX = cells.maxOfOrNull { it.x } ?: 0
        val maxY = cells.maxOfOrNull { it.y } ?: 0
        (maxX + 1) to (maxY + 1)
    }

    /**
     * Creates a copy of this piece with the same shape but different position.
     */
    fun withOffset(dx: Int, dy: Int): List<Cell> {
        return cells.map { it.copy(x = it.x + dx, y = it.y + dy) }
    }

    companion object {
        // Piece color definitions
        val COLORS = listOf(
            PieceColor.CORAL,
            PieceColor.TEAL,
            PieceColor.BLUE,
            PieceColor.GOLD,
            PieceColor.PINK,
            PieceColor.LAVENDER
        )
    }
}

/**
 * A cell in the game board or piece.
 */
data class Cell(val x: Int, val y: Int)
