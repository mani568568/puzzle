package com.hb.puzz.domain.model

/**
 * Represents the different piece colors used in Mosaic Blocks.
 */
enum class PieceColor(val hex: String) {
    CORAL("#FFB39A"),     // Pastel coral
    TEAL("#7DE3E8"),      // Pastel teal
    BLUE("#9BC5F2"),      // Soft blue
    GOLD("#FFD166"),      // Golden yellow
    PINK("#FAD6E8"),      // Light pink
    LAVENDER("#CBAACB");  // Muted lavender

    companion object {
        fun getRandomColor(exclude: List<PieceColor> = emptyList()): PieceColor {
            val available = (entries.toList() - exclude).ifEmpty { entries.toList() }
            return available.random()
        }
    }
}
