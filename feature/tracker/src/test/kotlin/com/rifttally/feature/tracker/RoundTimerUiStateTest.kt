package com.rifttally.feature.tracker

import com.rifttally.core.data.timer.RoundTimer
import kotlin.test.assertEquals
import org.junit.Test

class RoundTimerUiStateTest {
    private val running = RoundTimer(durationMs = 600_000, runningSinceMs = 0)

    @Test
    fun timeLeft_isRoundedUp() {
        assertEquals(RoundTimerUiState(true, 600, RoundTimerPhase.Normal), running.toUiState(0))
        assertEquals(600, running.toUiState(999).seconds)
        assertEquals(599, running.toUiState(1_000).seconds)
        assertEquals(1, running.toUiState(599_999).seconds)
    }

    @Test
    fun warning_fromFiveMinutesLeft() {
        assertEquals(RoundTimerPhase.Normal, running.toUiState(299_999).phase)
        assertEquals(RoundTimerUiState(true, 300, RoundTimerPhase.Warning), running.toUiState(300_000))
    }

    @Test
    fun overtime_fromZero_countsUpRoundedDown() {
        assertEquals(RoundTimerUiState(true, 0, RoundTimerPhase.Overtime), running.toUiState(600_000))
        assertEquals(0, running.toUiState(600_999).seconds)
        assertEquals(133, running.toUiState(733_000).seconds)
    }

    @Test
    fun minutesAndSeconds_parts() {
        val state = RoundTimerUiState(isRunning = false, seconds = 750, phase = RoundTimerPhase.Normal)
        assertEquals(12, state.minutesPart)
        assertEquals(30, state.secondsPart)
        // 120-minute rounds show three minute digits.
        assertEquals(120, RoundTimerUiState(false, 7_200, RoundTimerPhase.Normal).minutesPart)
    }

    @Test
    fun nextTick_landsOnTheNextDisplayedSecond() {
        assertEquals(1_000, running.millisUntilNextSecond(0))
        assertEquals(1, running.millisUntilNextSecond(999))
        assertEquals(1_000, running.millisUntilNextSecond(1_000))
        assertEquals(1_000, running.millisUntilNextSecond(600_000)) // at 0: next is +00:01
        assertEquals(700, running.millisUntilNextSecond(600_300))
    }
}
