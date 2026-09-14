package com.hb.puzz.domain

import kotlin.random.Random

data class PuzzleLevel(
    val id: Int,
    val title: String,
    /** Default/fixed grid size. Random-grid chapters use this as a safe fallback. */
    val gridSize: Int,
    val difficulty: Difficulty,
    val seed: Long? = null,
    /** When set, a fresh chapter can choose any square grid size in this range. */
    val randomGridSizes: IntRange? = null
) {
    enum class Difficulty(
        val displayLabel: String,
        val shortLabel: String
    ) {
        EASY("Easy · Cozy Start", "Cozy"),
        MEDIUM("Medium · Focus Flow", "Focus"),
        HARD("Hard · Master Quest", "Master")
    }

    val isRandomGrid: Boolean get() = randomGridSizes != null

    fun acceptsGridSize(size: Int): Boolean = randomGridSizes?.contains(size) ?: (size == gridSize)

    fun pickGridSize(random: Random = Random.Default): Int {
        val range = randomGridSizes ?: return gridSize
        return random.nextInt(range.first, range.last + 1)
    }

    val gridDescription: String
        get() = randomGridSizes?.let { range ->
            "Surprise Grid · ${range.first}×${range.first}–${range.last}×${range.last}"
        } ?: "${gridSize}×${gridSize} Grid"

    companion object {
        /**
         * Progression philosophy:
         *  - Chapters 1–5: 4×4 so players learn the merge mechanic without tiny tiles.
         *  - Chapters 6–8: 5×5.
         *  - Chapters 9–11: 6×6.
         *  - Chapters 12–14: 7×7.
         *  - Chapter 15: first full 8×8 challenge.
         *  - Chapters 16–19: surprise grids chosen from 6×6, 7×7, or 8×8 when the chapter starts.
         *  - Chapter 20: fixed 8×8 finale.
         */
        val ALL_LEVELS: List<PuzzleLevel> = listOf(
            PuzzleLevel(1, "Chipmunk", 4, Difficulty.EASY),
            PuzzleLevel(2, "Curious Cat", 4, Difficulty.EASY),
            PuzzleLevel(3, "Celebration Cake", 4, Difficulty.EASY),
            PuzzleLevel(4, "Lighthouse Coast", 4, Difficulty.EASY),
            PuzzleLevel(5, "Classic Racer", 4, Difficulty.EASY),

            PuzzleLevel(6, "Travel Table", 5, Difficulty.MEDIUM),
            PuzzleLevel(7, "Off-Road Beast", 5, Difficulty.MEDIUM),
            PuzzleLevel(8, "Emerald Eye", 5, Difficulty.MEDIUM),

            PuzzleLevel(9, "Red Rock Valley", 6, Difficulty.MEDIUM),
            PuzzleLevel(10, "City Lights", 6, Difficulty.MEDIUM),
            PuzzleLevel(11, "Warm Kitchen", 6, Difficulty.MEDIUM),

            PuzzleLevel(12, "Little Library", 7, Difficulty.HARD),
            PuzzleLevel(13, "Window Garden", 7, Difficulty.HARD),
            PuzzleLevel(14, "Autumn Walk", 7, Difficulty.HARD),
            PuzzleLevel(15, "Quiet Balcony", 8, Difficulty.HARD),

            PuzzleLevel(16, "Evening Street", 7, Difficulty.HARD, randomGridSizes = 6..8),
            PuzzleLevel(17, "Forest Cabin", 7, Difficulty.HARD, randomGridSizes = 6..8),
            PuzzleLevel(18, "Snowy Village", 7, Difficulty.HARD, randomGridSizes = 6..8),
            PuzzleLevel(19, "Moonlit Lake", 7, Difficulty.HARD, randomGridSizes = 6..8),
            PuzzleLevel(20, "Cozy Finale", 8, Difficulty.HARD)
        )

        fun getLevel(levelId: Int): PuzzleLevel? = ALL_LEVELS.find { it.id == levelId }

        fun requireLevel(levelId: Int): PuzzleLevel =
            getLevel(levelId) ?: ALL_LEVELS.first()

        val maxLevelId: Int get() = ALL_LEVELS.maxOf { it.id }
    }
}
