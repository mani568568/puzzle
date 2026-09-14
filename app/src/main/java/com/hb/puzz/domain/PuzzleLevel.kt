package com.hb.puzz.domain

data class PuzzleLevel(
    val id: Int,
    val title: String,
    val gridSize: Int,
    val difficulty: Difficulty,
    val seed: Long? = null
) {
    enum class Difficulty { EASY, MEDIUM, HARD }
    
    companion object {
        fun getLevel(levelId: Int): PuzzleLevel? {
            return ALL_LEVELS.find { it.id == levelId }
        }
        
        private val ALL_LEVELS = listOf(
            PuzzleLevel(1, "Morning Sun", 3, Difficulty.EASY),
            PuzzleLevel(2, "Teacup", 3, Difficulty.EASY),
            PuzzleLevel(3, "Cat Nap", 3, Difficulty.EASY),
            PuzzleLevel(4, "Garden Cottage", 4, Difficulty.MEDIUM),
            PuzzleLevel(5, "Reading Nook", 4, Difficulty.MEDIUM),
            PuzzleLevel(6, "Cozy Chair", 4, Difficulty.MEDIUM),
            PuzzleLevel(7, "Flower Pot", 4, Difficulty.MEDIUM),
            PuzzleLevel(8, "Mountain Lake", 5, Difficulty.HARD),
            PuzzleLevel(9, "Seaside Village", 5, Difficulty.HARD),
            PuzzleLevel(10, "Rainy Cafe", 5, Difficulty.HARD)
        )
    }
}
