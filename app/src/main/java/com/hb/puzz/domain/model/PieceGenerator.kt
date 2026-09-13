package com.hb.puzz.domain.model

import kotlin.random.Random

/**
 * Generates game pieces with various shapes.
 */
class PieceGenerator(private val random: Random = Random.Default) {

    companion object {
        // All possible piece shapes as relative cell coordinates
        private val SHAPES = listOf(
            // Single block (1x1)
            listOf(Cell(0, 0)),
            
            // Lines (1x2 to 1x5)
            listOf(Cell(0, 0), Cell(1, 0)),              // 2 blocks horizontal
            listOf(Cell(0, 0), Cell(0, 1)),              // 2 blocks vertical
            listOf(Cell(0, 0), Cell(1, 0), Cell(2, 0)),  // 3 blocks horizontal
            listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2)),  // 3 blocks vertical
            listOf(Cell(0, 0), Cell(1, 0), Cell(2, 0), Cell(3, 0)),  // 4 blocks horizontal
            listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2), Cell(0, 3)),  // 4 blocks vertical
            listOf(Cell(0, 0), Cell(1, 0), Cell(2, 0), Cell(3, 0), Cell(4, 0)),  // 5 horizontal
            listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2), Cell(0, 3), Cell(0, 4)),  // 5 vertical
            
            // Squares (2x2, 3x3)
            listOf(
                Cell(0, 0), Cell(1, 0),
                Cell(0, 1), Cell(1, 1)
            ),  // 2x2 square
            listOf(
                Cell(0, 0), Cell(1, 0), Cell(2, 0),
                Cell(0, 1), Cell(1, 1), Cell(2, 1),
                Cell(0, 2), Cell(1, 2), Cell(2, 2)
            ),  // 3x3 square
            
            // L shapes
            listOf(
                Cell(0, 0), Cell(0, 1), Cell(0, 2),
                Cell(1, 2)
            ),  // L shape 1
            listOf(
                Cell(0, 0), Cell(1, 0), Cell(2, 0),
                Cell(0, 1)
            ),  // L shape 2
            listOf(
                Cell(0, 0), Cell(1, 0),
                Cell(0, 1), Cell(0, 2)
            ),  // L shape 3
            listOf(
                Cell(2, 0), Cell(2, 1),
                Cell(0, 1), Cell(1, 1)
            ),  // L shape 4
            
            // T shapes
            listOf(
                Cell(0, 0), Cell(1, 0), Cell(2, 0),
                Cell(1, 1)
            ),  // T shape 1 (pointing down)
            listOf(
                Cell(0, 0), Cell(0, 1), Cell(0, 2),
                Cell(1, 1)
            ),  // T shape 2 (pointing right)
            listOf(
                Cell(1, 0),
                Cell(0, 1), Cell(1, 1), Cell(2, 1)
            ),  // T shape 3 (pointing up)
            listOf(
                Cell(1, 0), Cell(1, 1),
                Cell(0, 1), Cell(2, 1)
            ),  // T shape 4 (pointing left)
            
            // Zigzag shapes
            listOf(
                Cell(0, 0), Cell(1, 0),
                Cell(1, 1), Cell(2, 1)
            ),  // Z shape horizontal
            listOf(
                Cell(0, 0), Cell(0, 1),
                Cell(1, 1), Cell(1, 2)
            ),  // S shape vertical
            listOf(
                Cell(1, 0), Cell(2, 0),
                Cell(0, 1), Cell(1, 1)
            ),  // Reverse Z horizontal
            listOf(
                Cell(1, 0), Cell(1, 1),
                Cell(0, 1), Cell(0, 2)
            )   // Reverse S vertical
        )
    }

    /**
     * Generates a random piece with the specified ID and optional color.
     */
    fun generatePiece(id: Int, color: PieceColor? = null): Piece {
        val shape = SHAPES.random(random)
        return Piece(
            id = id,
            cells = shape,
            color = color ?: PieceColor.entries.random(random)
        )
    }

    /**
     * Generates three random pieces.
     */
    fun generatePieces(count: Int = 3): List<Piece> {
        val colorsUsed = mutableSetOf<PieceColor>()
        return (0 until count).map { id ->
            val pieceColor = (PieceColor.entries.toList() - colorsUsed).ifEmpty { PieceColor.entries.toList() }.random(random)
            colorsUsed.add(pieceColor)
            generatePiece(id, pieceColor)
        }
    }
}
