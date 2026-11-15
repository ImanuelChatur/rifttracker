package com.rifttally.core.stats

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

/** Placeholder-named seats ("Player 1") aren't people: left out of player stats, but their Legend counts. */
class PlaceholderNameTest {
    private val p1 = Seat("Player 1", legend = "jinx", default = true)
    private val p2 = Seat("Player 2", legend = "viktor", default = true)

    // Two unrelated tables whose blank seats both became "Player 1"; Ana wins both.
    private val games = List(MIN_GAMES_FOR_RANKING) { index ->
        ffa(Seat("Ana", legend = "ahri"), if (index % 2 == 0) p1 else p2, winner = 0, ended = index + 1L)
    } + ffa(p1, p2, winner = 1, ended = 100)

    @Test
    fun placeholders_areMissingFromPlayerStats_andTopPlayers() {
        assertEquals(listOf(key("ana")), playerStats(games).map { it.key })
        assertNull(playerStats(games, key("player 1")))
        assertEquals(listOf(key("ana")), topPlayersByWinRate(games).map { it.key })
        assertEquals(listOf(key("ana")), statsOverview(games).players.map { it.key })
    }

    @Test
    fun placeholders_areMissingFromHeadToHead() {
        assertTrue(headToHeads(games, key("ana")).isEmpty())
        assertEquals(0, headToHead(games, key("ana"), key("player 1")).games)
    }

    @Test
    fun placeholderLegends_stillCountInLegendStats() {
        val byId = legendStats(games).associateBy { it.legendId }
        val jinxGames = games.count { game -> game.config.players.any { it.legendId == "jinx" } }
        assertEquals(jinxGames, byId.getValue("jinx").games)
        // The placeholder-vs-placeholder game was won by seat 2 (Viktor).
        assertEquals(1, byId.getValue("viktor").wins)
    }
}
