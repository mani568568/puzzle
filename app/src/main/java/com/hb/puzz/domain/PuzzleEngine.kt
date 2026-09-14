package com.hb.puzz.domain

import kotlin.random.Random

class PuzzleEngine(
    gridSize: Int,
    seed: Long? = null
) {
    private val _gridSize: Int = gridSize
    private val totalTiles: Int = gridSize * gridSize
    private val random = seed?.let { Random(it) } ?: Random.Default
    
    var positions = IntArray(totalTiles) { it }
    
    init {
        shuffle()
    }
    
    val gridSize: Int get() = _gridSize
    fun getTotalTiles(): Int = totalTiles
    
    fun attemptSwap(posA: Int, posB: Int): Boolean {
        if (posA !in 0 until totalTiles || posB !in 0 until totalTiles) return false
        if (posA == posB) return false
        
        val temp = positions[posA]
        positions[posA] = positions[posB]
        positions[posB] = temp
        return true
    }
    
    fun isSolved(): Boolean {
        for (i in 0 until totalTiles) {
            if (positions[i] != i) return false
        }
        return true
    }
    
    fun shuffle() {
        for (i in totalTiles - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = positions[i]
            positions[i] = positions[j]
            positions[j] = temp
        }
        
        var attempts = 0
        while (isSolved() && attempts < 10) {
            shuffle()
            attempts++
        }
    }
    
    fun getTileAt(pos: Int): Int {
        return if (pos in 0 until totalTiles) positions[pos] else -1
    }
}
