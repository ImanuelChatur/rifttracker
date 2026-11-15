package com.rifttally.feature.tracker

import com.rifttally.core.data.timer.RoundTimer

/**
 * The round timer on the control bar, at whole-second resolution (so it only changes once a second).
 * [seconds] is the time left, rounded up (a fresh 50-minute timer shows 50:00), or in [RoundTimerPhase.Overtime]
 * the time past 0, rounded down ("+00:00" right at 0).
 */
internal data class RoundTimerUiState(val isRunning: Boolean, val seconds: Long, val phase: RoundTimerPhase) {
    val minutesPart: Long get() = seconds / SECONDS_PER_MINUTE
    val secondsPart: Long get() = seconds % SECONDS_PER_MINUTE
}

internal enum class RoundTimerPhase {
    Normal,

    /** 5 minutes or less left: warning color. */
    Warning,

    /** Time is up: counts up in the error color. */
    Overtime,
}

internal fun RoundTimer.toUiState(nowMs: Long): RoundTimerUiState {
    val remaining = remainingAt(nowMs)
    return if (remaining > 0) {
        RoundTimerUiState(
            isRunning = isRunning,
            seconds = (remaining + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND,
            phase = if (remaining <= WARNING_MS) RoundTimerPhase.Warning else RoundTimerPhase.Normal,
        )
    } else {
        RoundTimerUiState(
            isRunning = isRunning,
            seconds = -remaining / MILLIS_PER_SECOND,
            phase = RoundTimerPhase.Overtime,
        )
    }
}

/**
 * Millis from [nowMs] until a running timer's displayed second changes (1..1000), so the ticker wakes
 * once per second exactly on the boundary.
 */
internal fun RoundTimer.millisUntilNextSecond(nowMs: Long): Long =
    Math.floorMod(remainingAt(nowMs) - 1, MILLIS_PER_SECOND) + 1

/** Left ≤ this: [RoundTimerPhase.Warning]. */
internal const val WARNING_MS: Long = 5 * 60_000L

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
