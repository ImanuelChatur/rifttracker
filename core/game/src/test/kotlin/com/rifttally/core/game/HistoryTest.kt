package com.rifttally.core.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HistoryTest {
    private val e = Events()
    private val config = ffaConfig()

    @Test
    fun emptyEvents_emptyHistory() {
        assertTrue(history(config, emptyList()).isEmpty())
    }

    @Test
    fun eachEntry_holdsStateAfterItsEvent() {
        val events = listOf(e.points(S1, 2), e.points(S2, 1), e.points(S1, -5), e.target(10))

        val entries = history(config, events)

        assertEquals(events, entries.map { it.event })
        assertEquals(listOf(2, 2, 0, 0), entries.map { it.stateAfter.pointsOf(S1) })
        assertEquals(listOf(0, 1, 1, 1), entries.map { it.stateAfter.pointsOf(S2) })
        assertEquals(10, entries.last().stateAfter.targetScore)
    }

    @Test
    fun everyEntry_matchesReduceOfPrefix() {
        val events = listOf(
            e.points(S1, 3),
            e.addCounter(P1),
            e.adjustCounter(P1, 2),
            e.removeCounter(P1),
            e.target(5),
            e.end(S1),
            e.points(S2, 1),
        )

        history(config, events).forEachIndexed { index, entry ->
            assertEquals(reduce(config, events.take(index + 1)), entry.stateAfter)
        }
    }

    @Test
    fun lastEntry_equalsReduce() {
        val events = listOf(e.points(S1, 1), e.points(S2, 4), e.end(null))

        assertEquals(reduce(config, events), history(config, events).last().stateAfter)
    }
}
