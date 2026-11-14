package com.rifttally.core.stats

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import org.junit.Test

class SummarizeTest {
    private val s1 = sideId(0)
    private val s2 = sideId(1)

    @Test
    fun finalScores_winner_andDuration() {
        val game = ffa(
            ANA,
            BEN,
            winner = 0,
            ended = 25.minutes.inWholeMilliseconds,
            changes = listOf(PointChange(10, s1, 3), PointChange(20, s2, 2), PointChange(30, s1, 5)),
        )

        val summary = summarize(game)

        assertEquals(listOf(8, 2), summary.sides.map { it.finalScore })
        assertEquals(listOf(true, false), summary.sides.map { it.isWinner })
        assertEquals(s1, summary.winner)
        assertEquals(25.minutes, summary.duration)
        assertEquals(listOf("Ana", "Ben"), summary.sides.map { it.players.single().name })
        assertEquals(game.id, summary.gameId)
        assertEquals(game.config.format, summary.format)
        assertEquals(game.targetScore, summary.targetScore)
    }

    @Test
    fun timeline_startsAtZero_andHasOnePointPerChange() {
        val game = ffa(ANA, BEN, changes = listOf(PointChange(10, s1, 1), PointChange(20, s2, 4)))

        val timeline = summarize(game).timeline

        assertEquals(listOf(0L, 10L, 20L), timeline.map { it.timestamp })
        assertEquals(
            listOf(mapOf(s1 to 0, s2 to 0), mapOf(s1 to 1, s2 to 0), mapOf(s1 to 1, s2 to 4)),
            timeline.map { it.scores },
        )
    }

    @Test
    fun scoresAreFlooredAtZero_andSaturate() {
        val game = ffa(
            ANA,
            BEN,
            changes = listOf(
                PointChange(1, s1, -3),
                PointChange(2, s1, 2),
                PointChange(3, s2, Int.MAX_VALUE),
                PointChange(4, s2, 5),
            ),
        )

        val summary = summarize(game)

        assertEquals(listOf(0, 2, 2, 2), summary.timeline.drop(1).map { it.scores.getValue(s1) })
        assertEquals(listOf(2, Int.MAX_VALUE), summary.sides.map { it.finalScore })
    }

    @Test
    fun changesForUnknownSides_areIgnored() {
        val game = ffa(ANA, BEN, changes = listOf(PointChange(1, com.rifttally.core.model.SideId("x"), 5)))

        val summary = summarize(game)

        assertEquals(listOf(0, 0), summary.sides.map { it.finalScore })
        assertEquals(1, summary.timeline.size)
    }

    @Test
    fun noWinner_andUnknownWinner_meanNoWinner() {
        assertNull(summarize(ffa(ANA, BEN, winner = null)).winner)
        val unknown = ffa(ANA, BEN).copy(winner = com.rifttally.core.model.SideId("nobody"))
        val summary = summarize(unknown)
        assertNull(summary.winner)
        assertEquals(listOf(false, false), summary.sides.map { it.isWinner })
    }

    @Test
    fun negativeDuration_isZero() {
        val game = ffa(ANA, BEN, ended = 5).copy(createdAt = 10)
        assertEquals(Duration.ZERO, summarize(game).duration)
    }

    @Test
    fun teamGame_sideHoldsBothPlayers() {
        val summary = summarize(teams(ANA to BEN, CLEO to DAN, winner = TEAM_B))

        assertEquals(
            listOf(listOf("Ana", "Ben"), listOf("Cleo", "Dan")),
            summary.sides.map { s ->
                s.players.map { it.name }
            },
        )
        assertEquals(listOf(false, true), summary.sides.map { it.isWinner })
    }
}
