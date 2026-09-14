package com.hb.puzz.domain

import kotlin.math.abs

/** Saveable snapshot of a picture-puzzle session. */
data class PuzzleGameState(
    val levelId: Int,
    val tilePositions: IntArray,
    val moveCount: Int,
    val isSolved: Boolean
)

/** Keeps level metadata, puzzle state and move count together. */
class PuzzleStateManager(
    private val initialLevel: PuzzleLevel,
    initialPositions: IntArray? = null,
    initialMoveCount: Int = 0
) {
    private var engine = PuzzleEngine(initialLevel.gridSize, initialLevel.seed)
    var moveCount: Int = initialMoveCount
        private set

    init {
        initialPositions?.let { engine.restorePositions(it) }
    }

    val gridSize: Int get() = engine.gridSize

    fun getEngine(): PuzzleEngine = engine

    fun attemptSwap(positionA: Int, positionB: Int): Boolean {
        val swapped = engine.attemptSwap(positionA, positionB)
        if (swapped) moveCount++
        return swapped
    }

    fun isSolved(): Boolean = engine.isSolved()

    fun shuffle() {
        engine.shuffle()
        moveCount = 0
    }

    fun getConnectedGroups(): List<List<Int>> = engine.getConnectedGroups()

    fun createGameState(): PuzzleGameState = PuzzleGameState(
        levelId = initialLevel.id,
        tilePositions = engine.getCurrentPositions(),
        moveCount = moveCount,
        isSolved = isSolved()
    )

    fun restoreFromState(state: PuzzleGameState): Boolean {
        if (state.levelId != initialLevel.id) return false
        if (!engine.restorePositions(state.tilePositions)) return false
        moveCount = state.moveCount.coerceAtLeast(0)
        return true
    }
}

object PuzzleUtils {
    fun getPositionCoords(position: Int, gridSize: Int): Pair<Int, Int> =
        position / gridSize to position % gridSize

    fun getLinearPosition(row: Int, col: Int, gridSize: Int): Int = row * gridSize + col

    fun areAdjacent(posA: Int, posB: Int, gridSize: Int): Boolean {
        val (rowA, colA) = getPositionCoords(posA, gridSize)
        val (rowB, colB) = getPositionCoords(posB, gridSize)
        return (rowA == rowB && abs(colA - colB) == 1) ||
            (colA == colB && abs(rowA - rowB) == 1)
    }

    fun isValidPermutation(positions: IntArray, gridSize: Int): Boolean {
        val totalTiles = gridSize * gridSize
        if (positions.size != totalTiles) return false
        val seen = BooleanArray(totalTiles)
        for (tile in positions) {
            if (tile !in 0 until totalTiles || seen[tile]) return false
            seen[tile] = true
        }
        return true
    }
}
