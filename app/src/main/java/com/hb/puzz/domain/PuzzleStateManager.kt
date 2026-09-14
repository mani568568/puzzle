package com.hb.puzz.domain

import kotlin.math.abs

/**
 * Manages puzzle game state including engine, level info, and moves.
 */
class PuzzleStateManager(
    private val initialLevel: PuzzleLevel,
    initialPositions: IntArray? = null
) {
    private var _engine = PuzzleEngine(initialLevel.gridSize, initialLevel.seed)
    
    init {
        if (initialPositions != null && initialPositions.size == _engine.totalTiles) {
            // Validate and set provided positions
            val permCheck = BooleanArray(_engine.totalTiles) { false }
            var valid = true
            for (pos in initialPositions) {
                if (pos !in 0 until _engine.totalTiles || permCheck[pos]) {
                    valid = false
                    break
                }
                permCheck[pos] = true
            }
            
            if (valid) {
                _engine._tilePositions = initialPositions.clone()
            }
        }
    }
    
    /**
     * Gets the current grid size.
     */
    val gridSize: Int get() = _engine.gridSize
    
    /**
     * Gets the puzzle engine instance.
     */
    fun getEngine(): PuzzleEngine = _engine
    
    /**
     * Swaps two tiles and returns true if successful.
     */
    fun attemptSwap(positionA: Int, positionB: Int): Boolean {
        val swapped = _engine.attemptSwap(positionA, positionB)
        
        // Check if solved after swap
        return swapped
    }
    
    /**
     * Checks if the puzzle is currently solved.
     */
    fun isSolved(): Boolean = _engine.isSolved()
    
    /**
     * Shuffles all tiles (for restart).
     */
    fun shuffle() {
        _engine.shuffle()
    }
    
    /**
     * Gets connected groups of correctly adjacent tiles for visual merging.
     */
    fun getConnectedGroups(): List<List<Int>> = _engine.getConnectedGroups()
    
    /**
     * Creates a saveable state snapshot.
     */
    fun createGameState(): PuzzleGameState {
        return PuzzleGameState(
            level = initialLevel,
            tilePositions = _engine.getCurrentPositions(),
            moveCount = 0,  // Would be tracked separately
            isSolved = isSolved()
        )
    }
    
    /**
     * Restores state from a saved game.
     */
    fun restoreFromState(state: PuzzleGameState) {
        if (state.tilePositions.size == _engine.totalTiles) {
            _engine._tilePositions = state.tilePositions.clone()
        }
    }
}

/**
 * Utility functions for puzzle operations.
 */
object PuzzleUtils {
    
    /**
     * Gets the row and column from a linear position in a grid.
     */
    fun getPositionCoords(position: Int, gridSize: Int): Pair<Int, Int> {
        val row = position / gridSize
        val col = position % gridSize
        return row to col
    }
    
    /**
     * Converts row/column to linear position.
     */
    fun getLinearPosition(row: Int, col: Int, gridSize: Int): Int {
        return row * gridSize + col
    }
    
    /**
     * Checks if two positions are adjacent (horizontally or vertically).
     */
    fun areAdjacent(posA: Int, posB: Int, gridSize: Int): Boolean {
        val (rowA, colA) = getPositionCoords(posA, gridSize)
        val (rowB, colB) = getPositionCoords(posB, gridSize)
        
        return when {
            rowA == rowB && abs(colA - colB) == 1 -> true
            colA == colB && abs(rowA - rowB) == 1 -> true
            else -> false
        }
    }
    
    /**
     * Validates a tile permutation is valid (all IDs present exactly once).
     */
    fun isValidPermutation(positions: IntArray, gridSize: Int): Boolean {
        val totalTiles = gridSize * gridSize
        if (positions.size != totalTiles) return false
        
        val seen = BooleanArray(totalTiles) { false }
        for (pos in positions) {
            if (pos !in 0 until totalTiles || seen[pos]) {
                return false
            }
            seen[pos] = true
        }
        
        return seen.all { it }
    }
}
