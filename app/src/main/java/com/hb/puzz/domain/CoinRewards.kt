package com.hb.puzz.domain

import kotlin.math.roundToInt

/** Local game currency, with no cash value. Bonuses use fixed difficulty targets. */
data class CoinReward(val completion: Int, val speed: Int, val efficiency: Int) {
    val total: Int get() = completion + speed + efficiency
}

object CoinRewards {
    fun parMoves(grid: Int): Int = grid * grid + grid * (grid - 1)
    fun targetSeconds(grid: Int): Int = parMoves(grid) * 6

    fun calculate(grid: Int, elapsedMillis: Long, moves: Int): CoinReward {
        require(grid in 2..8)
        require(elapsedMillis >= 0 && moves >= 0)
        val base = grid * grid * 5
        val timeFraction = (1.0 - elapsedMillis.toDouble() / (targetSeconds(grid) * 1000.0)).coerceIn(0.0, 1.0)
        val moveFraction = ((2.0 * parMoves(grid) - moves) / parMoves(grid)).coerceIn(0.0, 1.0)
        return CoinReward(base, (base * timeFraction).roundToInt(), (base * moveFraction).roundToInt())
    }

    /** Replays pay only improvement over the chapter's previous best reward. */
    fun improvement(previousBest: Int, reward: CoinReward): Int =
        (reward.total - previousBest.coerceAtLeast(0)).coerceAtLeast(0)
}

/** Injectable monotonic clock makes pause/resume and timing independently testable. */
class ActivePlayClock(private val now: () -> Long) {
    private var accumulated = 0L
    private var resumedAt: Long? = null
    val running: Boolean get() = resumedAt != null
    fun elapsedMillis(): Long = accumulated + (resumedAt?.let { (now() - it).coerceAtLeast(0) } ?: 0)
    fun resume() { if (!running) resumedAt = now() }
    fun pause() { accumulated = elapsedMillis(); resumedAt = null }
    fun restore(millis: Long) { accumulated = millis.coerceAtLeast(0); resumedAt = null }
}
