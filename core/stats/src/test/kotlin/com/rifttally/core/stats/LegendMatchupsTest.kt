package com.rifttally.core.stats

import com.rifttally.core.model.SideId
import kotlin.test.assertEquals
import org.junit.Test

class LegendMatchupsTest {
    private val jinx = "jinx"
    private val viktor = "viktor"
    private val ahri = "ahri"
    private val zed = "zed"

    private fun matchup(other: String, games: Int, wins: Int, losses: Int) =
        LegendMatchup(jinx, other, games = games, wins = wins, losses = losses)

    @Test
    fun duels_countWinsAndLossesPerOpponent() {
        val games = listOf(
            ffa(Seat("Ana", jinx), Seat("Ben", viktor), winner = 0, ended = 1),
            ffa(Seat("Ana", jinx), Seat("Ben", viktor), winner = 1, ended = 2),
            ffa(Seat("Ben", viktor), Seat("Ana", jinx), winner = 1, ended = 3),
            ffa(Seat("Ana", jinx), Seat("Cleo", ahri), winner = null, ended = 4),
        )

        assertEquals(
            listOf(matchup(viktor, games = 3, wins = 2, losses = 1), matchup(ahri, games = 1, wins = 0, losses = 0)),
            legendMatchups(games, jinx),
        )
    }

    @Test
    fun mirrorMatch_isLeftOut() {
        val games = listOf(ffa(Seat("Ana", jinx), Seat("Ben", jinx), winner = 0))

        assertEquals(emptyList(), legendMatchups(games, jinx))
    }

    @Test
    fun mirrorInAFourPlayerGame_onlyDropsTheMirrorPair() {
        val game = ffa(Seat("Ana", jinx), Seat("Ben", jinx), Seat("Cleo", viktor), DAN, winner = 1)

        // Both Jinx picks meet Viktor, in one game; Ben's Jinx won it.
        assertEquals(listOf(matchup(viktor, games = 1, wins = 1, losses = 0)), legendMatchups(listOf(game), jinx))
    }

    @Test
    fun mirrorInAFourPlayerGame_wonByTheOpponent_isOneLoss() {
        val game = ffa(Seat("Ana", jinx), Seat("Ben", jinx), Seat("Cleo", viktor), DAN, winner = 2)

        assertEquals(listOf(matchup(viktor, games = 1, wins = 0, losses = 1)), legendMatchups(listOf(game), jinx))
    }

    @Test
    fun teammates_areNotOpponents_andEachOpponentCounts() {
        val game = teams(Seat("Ana", jinx) to Seat("Ben", viktor), Seat("Cleo", ahri) to Seat("Dan", zed), TEAM_B)

        assertEquals(
            listOf(matchup(ahri, games = 1, wins = 0, losses = 1), matchup(zed, games = 1, wins = 0, losses = 1)),
            legendMatchups(listOf(game), jinx),
        )
    }

    @Test
    fun sameLegendTwiceOnOneTeam_countsOneGame() {
        val game = teams(Seat("Ana", jinx) to Seat("Ben", jinx), Seat("Cleo", viktor) to DAN, TEAM_A)

        assertEquals(listOf(matchup(viktor, games = 1, wins = 1, losses = 0)), legendMatchups(listOf(game), jinx))
    }

    @Test
    fun opponentPickedTwiceOnTheOtherTeam_countsOneGame() {
        val game = teams(Seat("Ana", jinx) to BEN, Seat("Cleo", viktor) to Seat("Dan", viktor), TEAM_B)

        assertEquals(listOf(matchup(viktor, games = 1, wins = 0, losses = 1)), legendMatchups(listOf(game), jinx))
    }

    @Test
    fun opponentAlsoOnMyTeam_stillCountsOnce_andMyTeamsWinIsAWin() {
        // Viktor is both Jinx's teammate and an opponent: it still met Jinx across the table, once.
        val game = teams(Seat("Ana", jinx) to Seat("Ben", viktor), Seat("Cleo", viktor) to DAN, TEAM_A)

        assertEquals(listOf(matchup(viktor, games = 1, wins = 1, losses = 0)), legendMatchups(listOf(game), jinx))
    }

    @Test
    fun threePlayerGameWonBySomeoneElse_isNeitherWinNorLoss() {
        val game = ffa(Seat("Ana", jinx), Seat("Ben", viktor), Seat("Cleo", ahri), winner = 2)

        assertEquals(
            listOf(matchup(ahri, games = 1, wins = 0, losses = 1), matchup(viktor, games = 1, wins = 0, losses = 0)),
            legendMatchups(listOf(game), jinx),
        )
    }

    @Test
    fun unknownWinnerSide_countsAsNoWinner() {
        val game = ffa(Seat("Ana", jinx), Seat("Ben", viktor)).copy(winner = SideId("nobody"))

        assertEquals(listOf(matchup(viktor, games = 1, wins = 0, losses = 0)), legendMatchups(listOf(game), jinx))
    }

    @Test
    fun placeholderNamedPlayers_stillCount() {
        val game = ffa(Seat("Player 1", jinx, default = true), Seat("Player 2", viktor, default = true), winner = 0)

        assertEquals(listOf(matchup(viktor, games = 1, wins = 1, losses = 0)), legendMatchups(listOf(game), jinx))
    }

    @Test
    fun playersWithoutLegendOrSeat_areSkipped() {
        val base = ffa(Seat("Ana", jinx), BEN, Seat("Cleo", viktor), winner = 0)
        // Cleo is in the config but on no side (corrupt or partial data).
        val game = base.copy(config = base.config.copy(sides = base.config.sides.take(2)))

        assertEquals(emptyList(), legendMatchups(listOf(game), jinx))
        assertEquals(emptyList(), legendMatchups(listOf(game), viktor))
    }

    @Test
    fun legendNeverPicked_orNoGames_isEmpty() {
        assertEquals(emptyList(), legendMatchups(listOf(ffa(Seat("Ana", viktor), Seat("Ben", ahri))), jinx))
        assertEquals(emptyList(), legendMatchups(emptyList(), jinx))
    }

    @Test
    fun sortedByGames_thenWins_thenOpponentId() {
        val games = listOf(
            ffa(Seat("Ana", jinx), Seat("Ben", "c"), winner = 1, ended = 1),
            ffa(Seat("Ana", jinx), Seat("Ben", "b"), winner = 0, ended = 2),
            ffa(Seat("Ana", jinx), Seat("Ben", "a"), winner = 1, ended = 3),
            ffa(Seat("Ana", jinx), Seat("Ben", "d"), winner = 0, ended = 4),
            ffa(Seat("Ana", jinx), Seat("Ben", "d"), winner = 1, ended = 5),
        )

        assertEquals(listOf("d", "b", "a", "c"), legendMatchups(games, jinx).map { it.opponentLegendId })
    }

    @Test
    fun winRate_isWinsOverGames() {
        assertEquals(0.5, matchup(viktor, games = 4, wins = 2, losses = 1).winRate)
        assertEquals(0.0, matchup(viktor, games = 0, wins = 0, losses = 0).winRate)
    }
}
