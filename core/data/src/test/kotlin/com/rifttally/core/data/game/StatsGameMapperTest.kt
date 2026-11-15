package com.rifttally.core.data.game

import com.rifttally.core.game.GameEvent
import com.rifttally.core.model.CounterId
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
import com.rifttally.core.model.Player
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.Side
import com.rifttally.core.model.SideId
import com.rifttally.core.stats.PointChange
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

private val S1 = SideId("s1")
private val S2 = SideId("s2")

private val twoPlayers = GameConfig(
    format = GameFormat.TWO_PLAYERS,
    sides = listOf(Side(S1, listOf(PlayerId("p1"))), Side(S2, listOf(PlayerId("p2")))),
    players = listOf(
        Player(PlayerId("p1"), "Ana", "orange", legendId = "jinx"),
        Player(PlayerId("p2"), "Ben", "sky"),
    ),
)

class StatsGameMapperTest {
    @Test
    fun keepsOnlyPointChangesBeforeTheEnd_andTakesWinnerAndTargetFromTheLog() {
        val record = GameRecord(
            id = GameId("g"),
            config = twoPlayers,
            events = listOf(
                GameEvent.PointsAdjusted("e1", 10, S1, 2),
                GameEvent.CounterAdded("e2", 11, twoPlayers.players[0].id, CounterId("xp"), "XP"),
                GameEvent.TargetScoreChanged("e3", 12, 10),
                // An end naming an unknown side is ignored by the reducer, so play goes on.
                GameEvent.GameEnded("e4", 13, SideId("nobody")),
                GameEvent.PointsAdjusted("e5", 14, S2, 1),
                GameEvent.GameEnded("e6", 15, S2),
                GameEvent.PointsAdjusted("e7", 16, S1, 5),
            ),
            createdAt = 5,
            endedAt = 15,
        )

        val game = record.toStatsGame()

        assertEquals("g", game.id)
        assertEquals(listOf(PointChange(10, S1, 2), PointChange(14, S2, 1)), game.pointChanges)
        assertEquals(S2, game.winner)
        assertEquals(10, game.targetScore)
        assertEquals(5, game.createdAt)
        assertEquals(15, game.endedAt)
    }

    @Test
    fun firstPlayer_isTheLatestValidPickBeforeTheEnd() {
        val p1 = twoPlayers.players[0].id
        val p2 = twoPlayers.players[1].id
        val record = GameRecord(
            id = GameId("g"),
            config = twoPlayers,
            events = listOf(
                GameEvent.FirstPlayerChosen("e1", 1, p1),
                GameEvent.FirstPlayerChosen("e2", 2, p2),
                // Unknown player: ignored.
                GameEvent.FirstPlayerChosen("e3", 3, PlayerId("ghost")),
                GameEvent.GameEnded("e4", 4, S1),
                // After the end: ignored.
                GameEvent.FirstPlayerChosen("e5", 5, p1),
            ),
            createdAt = 0,
            endedAt = 4,
        )

        assertEquals(p2, record.toStatsGame().firstPlayer)
    }

    @Test
    fun noPick_hasNoFirstPlayer() {
        val record = GameRecord(GameId("g"), twoPlayers, listOf(GameEvent.GameEnded("e", 1, S1)), 0, 1)

        assertNull(record.toStatsGame().firstPlayer)
    }

    @Test
    fun noWinner_andMissingEndedAt_fallsBackToCreatedAt() {
        val record = GameRecord(GameId("g"), twoPlayers, emptyList(), createdAt = 7, endedAt = null)

        val game = record.toStatsGame()

        assertNull(game.winner)
        assertEquals(7, game.endedAt)
    }
}
