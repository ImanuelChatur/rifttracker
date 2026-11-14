package com.rifttally.core.testing

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** A controllable [Clock] for tests. Starts at [start] and only moves when advanced. */
@OptIn(ExperimentalTime::class)
public class TestClock(start: Instant = Instant.fromEpochMilliseconds(0)) : Clock {
    private var current: Instant = start

    override fun now(): Instant = current

    public fun advanceBy(duration: Duration) {
        current += duration
    }

    public fun setTo(instant: Instant) {
        current = instant
    }
}
