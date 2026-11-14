package com.rifttally.core.game

import com.rifttally.core.model.CounterId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class GameEventFactoryTest {
    private var now: Instant = Instant.fromEpochMilliseconds(1_000)
    private val clock = object : Clock {
        override fun now(): Instant = now
    }
    private var nextId = 0
    private val factory = GameEventFactory(clock) { "id${++nextId}" }

    @Test
    fun pointsAdjusted() {
        assertEquals(GameEvent.PointsAdjusted("id1", 1_000, S1, -1), factory.pointsAdjusted(S1, -1))
    }

    @Test
    fun counterAdded_generatesCounterId() {
        val event = factory.counterAdded(P1, "XP")

        assertEquals(GameEvent.CounterAdded("id2", 1_000, P1, CounterId("id1"), "XP"), event)
    }

    @Test
    fun counterAdded_withGivenCounterId() {
        assertEquals(GameEvent.CounterAdded("id1", 1_000, P1, XP, "XP"), factory.counterAdded(P1, "XP", XP))
    }

    @Test
    fun counterAdjusted() {
        assertEquals(GameEvent.CounterAdjusted("id1", 1_000, P2, XP, 2), factory.counterAdjusted(P2, XP, 2))
    }

    @Test
    fun counterRemoved() {
        assertEquals(GameEvent.CounterRemoved("id1", 1_000, P2, XP), factory.counterRemoved(P2, XP))
    }

    @Test
    fun targetScoreChanged() {
        assertEquals(GameEvent.TargetScoreChanged("id1", 1_000, 11), factory.targetScoreChanged(11))
    }

    @Test
    fun gameEnded() {
        assertEquals(GameEvent.GameEnded("id1", 1_000, S1), factory.gameEnded(S1))
        assertEquals(GameEvent.GameEnded("id2", 1_000, null), factory.gameEnded(null))
    }

    @Test
    fun usesCurrentClockTime() {
        val first = factory.pointsAdjusted(S1, 1)
        now += 5.seconds
        val second = factory.pointsAdjusted(S1, 1)

        assertEquals(1_000, first.timestamp)
        assertEquals(6_000, second.timestamp)
    }

    @Test
    fun defaultIdGenerator_producesUniqueUuids() {
        val defaultFactory = GameEventFactory(clock)
        val a = defaultFactory.pointsAdjusted(S1, 1)
        val b = defaultFactory.pointsAdjusted(S1, 1)

        assertNotEquals(a.id, b.id)
        assertEquals(36, a.id.length)
    }
}
