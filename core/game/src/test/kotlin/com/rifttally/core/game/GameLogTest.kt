package com.rifttally.core.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameLogTest {
    private val e = Events()
    private val config = ffaConfig()
    private val names = mapOf(S1 to "Ana", S2 to "Ben")

    private fun log(vararg events: GameEvent) = gameLog(config, history(config, events.toList()), names)

    @Test
    fun noEvents_emptyLog() {
        assertTrue(log().isEmpty())
    }

    @Test
    fun everyEventKind_becomesALine_oldestFirst() {
        val lines = log(
            e.points(S1, 2),
            e.addCounter(P2),
            e.adjustCounter(P2, 3),
            e.removeCounter(P2),
            e.target(10),
            e.end(S1),
        )

        assertEquals(
            listOf(
                GameLogLine.Points("e1", "Ana", 2, 2),
                GameLogLine.CounterAdded("e2", "Player p2", "XP"),
                GameLogLine.CounterChanged("e3", "Player p2", "XP", 3, 3),
                GameLogLine.CounterRemoved("e4", "Player p2", "XP"),
                GameLogLine.TargetChanged("e5", 10),
                GameLogLine.GameEnded("e6", "Ana"),
            ),
            lines,
        )
    }

    @Test
    fun eventsThatChangeNothing_areDropped() {
        val lines = log(e.points(UNKNOWN_SIDE, 1), e.end(null), e.points(S1, 1))

        assertEquals(listOf<GameLogLine>(GameLogLine.GameEnded("e2", null)), lines)
    }

    @Test
    fun firstPlayerPick_becomesALine_evenWhenTheSamePlayerIsPickedAgain() {
        val lines = log(e.first(P1), e.first(P1), e.first(UNKNOWN_PLAYER), e.end(null), e.first(P2))

        assertEquals(
            listOf(
                GameLogLine.FirstPlayer("e1", "Player p1"),
                GameLogLine.FirstPlayer("e2", "Player p1"),
                GameLogLine.GameEnded("e4", null),
            ),
            lines,
        )
    }

    @Test
    fun sideWithoutName_isDropped() {
        val lines = gameLog(config, history(config, listOf(e.points(S2, 1))), mapOf(S1 to "Ana"))

        assertTrue(lines.isEmpty())
    }
}
