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
        val ALL_LEVELS: List<PuzzleLevel> = listOf(
            PuzzleLevel(1, "Morning Sun", 3, Difficulty.EASY),
            PuzzleLevel(2, "Teacup", 3, Difficulty.EASY),
            PuzzleLevel(3, "Cat Nap", 3, Difficulty.EASY),
            PuzzleLevel(4, "Garden Cottage", 4, Difficulty.MEDIUM),
            PuzzleLevel(5, "Reading Nook", 4, Difficulty.MEDIUM),
            PuzzleLevel(6, "Cozy Chair", 4, Difficulty.MEDIUM),
            PuzzleLevel(7, "Flower Pot", 4, Difficulty.MEDIUM),
            PuzzleLevel(8, "Mountain Lake", 5, Difficulty.HARD),
            PuzzleLevel(9, "Seaside Village", 5, Difficulty.HARD),
            PuzzleLevel(10, "Rainy Cafe", 5, Difficulty.HARD),
            PuzzleLevel(11, "Warm Kitchen", 3, Difficulty.EASY),
            PuzzleLevel(12, "Little Library", 3, Difficulty.EASY),
            PuzzleLevel(13, "Window Garden", 4, Difficulty.MEDIUM),
            PuzzleLevel(14, "Autumn Walk", 4, Difficulty.MEDIUM),
            PuzzleLevel(15, "Quiet Balcony", 4, Difficulty.MEDIUM),
            PuzzleLevel(16, "Evening Street", 5, Difficulty.HARD),
            PuzzleLevel(17, "Forest Cabin", 5, Difficulty.HARD),
            PuzzleLevel(18, "Snowy Village", 5, Difficulty.HARD),
            PuzzleLevel(19, "Moonlit Lake", 5, Difficulty.HARD),
            PuzzleLevel(20, "Cozy Finale", 5, Difficulty.HARD)
        )

        fun getLevel(levelId: Int): PuzzleLevel? = ALL_LEVELS.find { it.id == levelId }

        fun requireLevel(levelId: Int): PuzzleLevel =
            getLevel(levelId) ?: ALL_LEVELS.first()

        val maxLevelId: Int get() = ALL_LEVELS.maxOf { it.id }
    }
}
