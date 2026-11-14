package com.rifttally.core.game

import com.rifttally.core.model.Counter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class UndoTest {
    private val e = Events()
    private val config = ffaConfig(playerCount = 3, initialCounters = mapOf(P2 to listOf(Counter(XP, "XP", 1))))

    /** A game in progress: S1 has 3 points, P1 has an XP counter at 2, target is 10. */
    private val history = listOf(
        e.points(S1, 3),
        e.addCounter(P1),
        e.adjustCounter(P1, 2),
        e.target(10),
    )

    private fun assertUndoRestoresPreviousState(event: GameEvent) {
        val before = reduce(config, history)
        val after = reduce(config, history + event)
        assertNotEquals(before, after, "precondition: $event must change the state")

        val undone = (history + event).undo()

        assertEquals(history, undone)
        assertEquals(before, reduce(config, undone))
    }

    @Test
    fun undo_pointsIncrease() = assertUndoRestoresPreviousState(e.points(S1, 1))

    @Test
    fun undo_pointsDecrease() = assertUndoRestoresPreviousState(e.points(S1, -2))

    @Test
    fun undo_pointsDecreaseThatWasFloored() {
        // S1 has 3; -5 floors to 0. Undo must bring back 3 (not -5 + 5 arithmetic).
        assertUndoRestoresPreviousState(e.points(S1, -5))
    }

    @Test
    fun undo_counterAdded() = assertUndoRestoresPreviousState(e.addCounter(P3))

    @Test
    fun undo_counterAdjusted() = assertUndoRestoresPreviousState(e.adjustCounter(P1, 1))

    @Test
    fun undo_counterAdjustedThatWasFloored() = assertUndoRestoresPreviousState(e.adjustCounter(P1, -10))

    @Test
    fun undo_initialCounterAdjusted() = assertUndoRestoresPreviousState(e.adjustCounter(P2, 4))

    @Test
    fun undo_counterRemoved_restoresValueAndPosition() = assertUndoRestoresPreviousState(e.removeCounter(P1))

    @Test
    fun undo_initialCounterRemoved() = assertUndoRestoresPreviousState(e.removeCounter(P2))

    @Test
    fun undo_targetScoreChanged() = assertUndoRestoresPreviousState(e.target(4))

    @Test
    fun undo_gameEndedWithWinner() = assertUndoRestoresPreviousState(e.end(S1))

    @Test
    fun undo_gameEndedWithoutWinner() = assertUndoRestoresPreviousState(e.end(null))

    @Test
    fun undo_emptyListIsNoOp() {
        val empty = emptyList<GameEvent>()

        assertEquals(empty, empty.undo())
        assertEquals(reduce(config, emptyList()), reduce(config, empty.undo()))
    }

    @Test
    fun undo_repeatedly_walksBackToStart() {
        var events: List<GameEvent> = history
        repeat(history.size) { events = events.undo() }

        assertEquals(emptyList(), events)
        assertEquals(reduce(config, emptyList()), reduce(config, events))
        assertEquals(emptyList(), events.undo())
    }

    @Test
    fun undo_removesOnlyTheLastEvent() {
        val events = history + e.points(S2, 1) + e.points(S2, 1)

        assertEquals(1, reduce(config, events.undo()).pointsOf(S2))
    }

    @Test
    fun undo_doesNotMutateOriginal() {
        val events = history.toList()
        events.undo()

        assertEquals(history, events)
    }
}
