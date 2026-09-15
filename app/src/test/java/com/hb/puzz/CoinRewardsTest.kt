package com.hb.puzz

import com.hb.puzz.domain.CoinRewards
import com.hb.puzz.domain.ActivePlayClock
import org.junit.Assert.*
import org.junit.Test

class CoinRewardsTest {
    @Test fun `faster completion earns more coins with equal moves`() {
        val fast = CoinRewards.calculate(6, 120_000, 60)
        val slow = CoinRewards.calculate(6, 300_000, 60)
        assertTrue(fast.total > slow.total)
        assertEquals(fast.efficiency, slow.efficiency)
    }
    @Test fun `fewer moves earns more coins with equal time`() {
        assertTrue(CoinRewards.calculate(4, 60_000, 28).total > CoinRewards.calculate(4, 60_000, 50).total)
    }
    @Test fun `slow inefficient completion still earns base coins`() {
        val reward = CoinRewards.calculate(8, Long.MAX_VALUE, Int.MAX_VALUE)
        assertEquals(320, reward.total)
        assertEquals(0, reward.speed)
        assertEquals(0, reward.efficiency)
    }
    @Test fun `repeating or worsening best result cannot mint more coins`() {
        val reward = CoinRewards.calculate(5, 90_000, 40)
        assertEquals(0, CoinRewards.improvement(reward.total, reward))
        assertEquals(0, CoinRewards.improvement(reward.total + 10, reward))
        assertEquals(20, CoinRewards.improvement(reward.total - 20, reward))
    }
    @Test fun `reward stays bounded for every supported grid`() {
        for (grid in 4..8) {
            val reward = CoinRewards.calculate(grid, 0, 0)
            assertEquals(3 * grid * grid * 5, reward.total)
        }
    }
    @Test fun `pause excludes time away and resume does not reset time`() {
        var now = 1000L
        val clock = ActivePlayClock { now }
        clock.resume(); now += 2750; clock.pause()
        now += 60_000
        assertEquals(2750L, clock.elapsedMillis())
        clock.resume(); now += 1250
        assertEquals(4000L, clock.elapsedMillis())
    }
    @Test fun `repeated resume and pause are idempotent`() {
        var now = 0L
        val clock = ActivePlayClock { now }
        clock.resume(); now = 1000; clock.resume(); now = 2000
        clock.pause(); clock.pause()
        assertEquals(2000L, clock.elapsedMillis())
    }
    @Test fun `restored clock starts paused`() {
        val clock = ActivePlayClock { 500_000 }
        clock.restore(12_345)
        assertFalse(clock.running)
        assertEquals(12_345L, clock.elapsedMillis())
    }
}
