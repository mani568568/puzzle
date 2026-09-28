package com.hb.puzz.domain

import kotlin.random.Random

data class PuzzleLevel(
    val id: Int,
    val title: String,
    /** Default/fixed grid size. Random-grid chapters use this as a safe fallback. */
    val gridSize: Int,
    val difficulty: Difficulty,
    val seed: Long? = null,
    /** When set, a fresh chapter can choose any rectangular difficulty preset in this range. */
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

    fun acceptsBaseGridSize(size: Int): Boolean = randomGridSizes?.contains(size) ?: (size == gridSize)

    fun acceptsSessionGridSize(baseGridSize: Int, size: Int): Boolean =
        acceptsBaseGridSize(baseGridSize) &&
            size in maxOf(MIN_GRID_SIZE, baseGridSize - 2)..baseGridSize

    fun acceptsGridSize(size: Int): Boolean {
        val baseRange = randomGridSizes ?: (gridSize..gridSize)
        return size in maxOf(MIN_GRID_SIZE, baseRange.first - 2)..baseRange.last
    }

    fun pickGridSize(random: Random = Random.Default): Int {
        val range = randomGridSizes ?: return gridSize
        return random.nextInt(range.first, range.last + 1)
    }

    val gridDescription: String
        get() = randomGridSizes?.let { range ->
            val firstCols = PuzzleEngine.columnsForSize(range.first)
            val firstRows = PuzzleEngine.rowsForSize(range.first)
            val lastCols = PuzzleEngine.columnsForSize(range.last)
            val lastRows = PuzzleEngine.rowsForSize(range.last)
            "Surprise Grid · ${firstCols}×${firstRows}–${lastCols}×${lastRows}"
        } ?: "${PuzzleEngine.columnsForSize(gridSize)}×${PuzzleEngine.rowsForSize(gridSize)} Grid"

    companion object {
        const val MIN_GRID_SIZE: Int = 2
        const val MAX_GRID_SIZE: Int = 8

        /**
         * drawable-nodpi contains level_001.jpeg ... level_999.jpeg, therefore levels are generated
         * instead of hard-coded. This keeps lookup O(1), avoids a 999-entry source file, and makes it
         * straightforward to extend the catalog later.
         */
        const val MAX_LEVEL_ID: Int = 999

        private val originalTitles = listOf(
            "Chipmunk", "Curious Cat", "Celebration Cake", "Lighthouse Coast", "Classic Racer",
            "Travel Table", "Off-Road Beast", "Emerald Eye", "Red Rock Valley", "City Lights",
            "Warm Kitchen", "Little Library", "Window Garden", "Autumn Walk", "Quiet Balcony",
            "Evening Street", "Forest Cabin", "Snowy Village", "Moonlit Lake", "Cozy Finale"
        )

        /** Preserve the original first twenty puzzle settings for existing saves. */
        private fun firstTwentyGrid(levelId: Int): Int = when (levelId) {
            in 1..5 -> 4
            in 6..8 -> 5
            in 9..11 -> 6
            in 12..14 -> 7
            15 -> 8
            in 16..19 -> 7
            20 -> 8
            else -> error("Only valid for the first twenty levels")
        }

        /**
         * After level 20, difficulty moves through five-level bands of 4..8. This avoids keeping
         * hundreds of levels permanently at the maximum tile count while still providing variety.
         */
        private fun generatedGrid(levelId: Int): Int =
            4 + (((levelId - 21) / 5) % 5)

        private fun difficultyFor(gridSize: Int): Difficulty = when (gridSize) {
            in MIN_GRID_SIZE..4 -> Difficulty.EASY
            5, 6 -> Difficulty.MEDIUM
            else -> Difficulty.HARD
        }

        private fun buildLevel(levelId: Int): PuzzleLevel {
            val gridSize = if (levelId <= 20) firstTwentyGrid(levelId) else generatedGrid(levelId)
            val title = originalTitles.getOrNull(levelId - 1) ?: "Puzzle ${levelId.toString().padStart(3, '0')}"
            val randomRange = if (levelId in 16..19) 6..8 else null
            return PuzzleLevel(
                id = levelId,
                title = title,
                gridSize = gridSize,
                difficulty = difficultyFor(gridSize),
                randomGridSizes = randomRange
            )
        }

        val ALL_LEVELS: List<PuzzleLevel> by lazy(LazyThreadSafetyMode.PUBLICATION) {
            (1..MAX_LEVEL_ID).map(::buildLevel)
        }

        fun getLevel(levelId: Int): PuzzleLevel? =
            if (levelId in 1..MAX_LEVEL_ID) ALL_LEVELS[levelId - 1] else null

        fun requireLevel(levelId: Int): PuzzleLevel =
            getLevel(levelId) ?: ALL_LEVELS.first()

        val maxLevelId: Int get() = MAX_LEVEL_ID
    }
}
