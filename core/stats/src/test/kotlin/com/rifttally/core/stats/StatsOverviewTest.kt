package com.rifttally.core.stats

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class StatsOverviewTest {
    @Test
    fun overview_totalsMostPlayedLegendAndRankings() {
        val games = listOf(
            ffa(Seat("Ana", "jinx"), Seat("Ben", "viktor"), winner = 0, ended = 1),
            ffa(Seat("Ana", "jinx"), BEN, winner = 0, ended = 2),
            ffa(ANA, BEN, winner = 1, ended = 3),
            ffa(ANA, CLEO, winner = null, ended = 4),
        )

        val overview = statsOverview(games)

        assertEquals(4, overview.totalGames)
        assertEquals(LegendStats("jinx", 2, 2), overview.mostPlayedLegend)
        assertEquals(listOf("Ana", "Ben"), overview.topPlayers.map { it.name })
        assertEquals(listOf("Ana", "Ben", "Cleo"), overview.players.map { it.name })
    }

    @Test
    fun emptyOverview() {
        val overview = statsOverview(emptyList())

        assertEquals(0, overview.totalGames)
        assertNull(overview.mostPlayedLegend)
        assertEquals(emptyList(), overview.topPlayers)
        assertEquals(emptyList(), overview.players)
    }

    @Test
    fun noLegendsPicked_noMostPlayedLegend() {
        assertNull(statsOverview(listOf(ffa(ANA, BEN))).mostPlayedLegend)
    }
}
