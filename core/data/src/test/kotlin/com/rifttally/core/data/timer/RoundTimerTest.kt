package com.rifttally.core.data.timer

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Test

class RoundTimerTest {
    private val fifty = RoundTimer.ofMinutes(50)

    @Test
    fun fresh_isPausedAtFullLength() {
        assertEquals(50 * 60_000L, fifty.durationMs)
        assertFalse(fifty.isRunning)
        assertEquals(50 * 60_000L, fifty.remainingAt(nowMs = 123_456))
    }

    @Test
    fun paused_doesNotMoveWithTheClock() {
        val paused = RoundTimer(durationMs = 60_000, elapsedBeforeMs = 15_000)
        assertEquals(45_000, paused.remainingAt(0))
        assertEquals(45_000, paused.remainingAt(10_000_000))
    }

    @Test
    fun running_countsDownFromWhenItStarted() {
        val running = RoundTimer(durationMs = 60_000, elapsedBeforeMs = 15_000, runningSinceMs = 1_000)
        assertEquals(45_000, running.remainingAt(1_000))
        assertEquals(35_000, running.remainingAt(11_000))
    }

    @Test
    fun overtime_isNegativeRemaining() {
        val running = RoundTimer(durationMs = 60_000, runningSinceMs = 0)
        assertEquals(0, running.remainingAt(60_000))
        assertEquals(-133_000, running.remainingAt(193_000))
    }

    @Test
    fun clockGoingBackwards_neverAddsNegativeTime() {
        val running = RoundTimer(durationMs = 60_000, elapsedBeforeMs = 5_000, runningSinceMs = 10_000)
        assertEquals(5_000, running.elapsedAt(2_000))
        assertEquals(5_000, running.pausedAt(2_000).elapsedBeforeMs)
    }

    @Test
    fun startPauseStart_banksEachRun() {
        val t1 = fifty.startedAt(1_000)
        assertTrue(t1.isRunning)
        val t2 = t1.pausedAt(31_000)
        assertEquals(RoundTimer(fifty.durationMs, elapsedBeforeMs = 30_000), t2)
        val t3 = t2.startedAt(100_000)
        assertEquals(fifty.durationMs - 40_000, t3.remainingAt(110_000))
    }

    @Test
    fun startWhileRunning_andPauseWhilePaused_changeNothing() {
        val running = fifty.startedAt(1_000)
        assertSame(running, running.startedAt(5_000))
        assertSame(fifty, fifty.pausedAt(5_000))
    }

    @Test
    fun reset_isFullLengthAndPaused() {
        val used = RoundTimer(durationMs = 60_000, elapsedBeforeMs = 70_000, runningSinceMs = 3)
        assertEquals(RoundTimer(durationMs = 60_000), used.reset())
    }
}
