package com.rifttally.core.data.timer

/**
 * A game's round timer as stored: [durationMs] long, [elapsedBeforeMs] banked by earlier runs, and
 * [runningSinceMs] (epoch millis) while it runs. The live values depend on "now", which callers take from
 * the injected clock. It is not an event: Undo never changes it.
 */
public data class RoundTimer(val durationMs: Long, val elapsedBeforeMs: Long = 0, val runningSinceMs: Long? = null) {
    val isRunning: Boolean get() = runningSinceMs != null

    /** Total elapsed time at [nowMs]. A clock that moved backwards adds nothing (never negative). */
    public fun elapsedAt(nowMs: Long): Long =
        elapsedBeforeMs + (runningSinceMs?.let { (nowMs - it).coerceAtLeast(0) } ?: 0)

    /** `duration − elapsed` at [nowMs]: positive while time is left, ≤ 0 in overtime. */
    public fun remainingAt(nowMs: Long): Long = durationMs - elapsedAt(nowMs)

    /** Running from [nowMs] (unchanged if already running). */
    public fun startedAt(nowMs: Long): RoundTimer = if (isRunning) this else copy(runningSinceMs = nowMs)

    /** Paused at [nowMs], with the run banked (unchanged if already paused). */
    public fun pausedAt(nowMs: Long): RoundTimer =
        if (isRunning) copy(elapsedBeforeMs = elapsedAt(nowMs), runningSinceMs = null) else this

    /** Full duration, paused. */
    public fun reset(): RoundTimer = copy(elapsedBeforeMs = 0, runningSinceMs = null)

    public companion object {
        /** A fresh timer: full [minutes], paused (the table starts it when the round begins). */
        public fun ofMinutes(minutes: Int): RoundTimer = RoundTimer(durationMs = minutes * MILLIS_PER_MINUTE)

        public const val MILLIS_PER_MINUTE: Long = 60_000L
    }
}
