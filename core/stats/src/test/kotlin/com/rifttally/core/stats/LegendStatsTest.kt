package com.rifttally.core.stats

import kotlin.test.assertEquals
import org.junit.Test

class LegendStatsTest {
    private val jinx = "jinx"
    private val viktor = "viktor"

    @Test
    fun gamesAndWinRate_perLegend_skippingPlayersWithoutLegend() {
        val games = listOf(
            ffa(Seat("Ana", jinx), Seat("Ben", viktor), winner = 0, ended = 1),
            ffa(Seat("Ana", jinx), Seat("Ben", viktor), winner = 1, ended = 2),
            ffa(Seat("Ana", jinx), BEN, winner = 0, ended = 3),
            ffa(Seat("Ana", jinx), BEN, winner = null, ended = 4),
        )

        val stats = legendStats(games)

        assertEquals(listOf(LegendStats(jinx, 4, 2), LegendStats(viktor, 2, 1)), stats)
        assertEquals(0.5, stats[0].winRate)
    }

    @Test
    fun mirrorMatch_countsEachPick() {
        val stats = legendStats(listOf(ffa(Seat("Ana", jinx), Seat("Ben", jinx), winner = 1)))

        assertEquals(listOf(LegendStats(jinx, 2, 1)), stats)
    }

    @Test
    fun teamWin_countsForBothTeammatesLegends() {
        val game = teams(Seat("Ana", jinx) to Seat("Ben", viktor), Seat("Cleo", "ahri") to DAN, winner = TEAM_A)

        val byId = legendStats(listOf(game)).associateBy { it.legendId }

        assertEquals(1, byId.getValue(jinx).wins)
        assertEquals(1, byId.getValue(viktor).wins)
        assertEquals(0, byId.getValue("ahri").wins)
        assertEquals(3, byId.size)
    }

    @Test
    fun sortedByGames_thenWinRate_thenId() {
        val games = listOf(
            ffa(Seat("Ana", "b"), Seat("Ben", "a"), winner = 0, ended = 1),
            ffa(Seat("Ana", "c"), Seat("Ben", "a"), winner = 0, ended = 2),
            ffa(Seat("Ana", "d"), Seat("Ben", "e"), winner = null, ended = 3),
        )

        assertEquals(listOf("a", "b", "c", "d", "e"), legendStats(games).map { it.legendId })
    }

    @Test
    fun noGames_noStats_andZeroGamesIsZeroRate() {
        assertEquals(emptyList(), legendStats(emptyList()))
        assertEquals(0.0, LegendStats(jinx, 0, 0).winRate)
    }
}
