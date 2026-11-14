package com.rifttally.core.stats

import kotlin.test.assertEquals
import org.junit.Test

class HeadToHeadTest {
    @Test
    fun winsEach_andNoWinnerGamesCountAsPlayed() {
        val games = listOf(
            ffa(ANA, BEN, winner = 0, ended = 1),
            ffa(ANA, BEN, winner = 0, ended = 2),
            ffa(BEN, ANA, winner = 0, ended = 3),
            ffa(ANA, BEN, winner = null, ended = 4),
            ffa(ANA, CLEO, winner = 1, ended = 5),
        )

        val h2h = headToHead(games, key("ana"), key("ben"))

        assertEquals(4, h2h.games)
        assertEquals(2, h2h.wins)
        assertEquals(1, h2h.opponentWins)
        assertEquals("Ben", h2h.opponentName)
    }

    @Test
    fun threePlayerGame_wonBySomeoneElse_isALossForNeither() {
        val games = listOf(ffa(ANA, BEN, CLEO, winner = 2))

        val h2h = headToHead(games, key("ana"), key("ben"))

        assertEquals(1, h2h.games)
        assertEquals(0, h2h.wins)
        assertEquals(0, h2h.opponentWins)
    }

    @Test
    fun teammatesAreNotOpponents() {
        val games = listOf(teams(ANA to BEN, CLEO to DAN, winner = TEAM_A))

        val all = headToHeads(games, key("ana"))

        assertEquals(listOf("Cleo", "Dan"), all.map { it.opponentName })
        all.forEach {
            assertEquals(1, it.wins)
            assertEquals(0, it.opponentWins)
        }
        assertEquals(0, headToHead(games, key("ana"), key("ben")).games)
    }

    @Test
    fun headToHeads_listsEveryOpponent_mostGamesFirst_withLatestName() {
        val games = listOf(
            ffa(ANA, Seat("cleo"), ended = 1),
            ffa(ANA, BEN, ended = 2),
            ffa(ANA, BEN, ended = 3),
            ffa(ANA, CLEO, ended = 4),
            ffa(BEN, CLEO, ended = 5),
        )

        val all = headToHeads(games, key("ana"))

        assertEquals(listOf("Ben" to 2, "Cleo" to 2), all.map { it.opponentName to it.games })
        assertEquals(key("ana"), all.first().player)
    }

    @Test
    fun neverMet_isAllZero() {
        val games = listOf(ffa(ANA, BEN), ffa(CLEO, DAN))

        val h2h = headToHead(games, key("ana"), key("cleo"))

        assertEquals(HeadToHead(key("ana"), key("cleo"), "Cleo", 0, 0, 0), h2h)
        assertEquals(emptyList(), headToHeads(games, key("zed")))
    }
}
