package com.rifttally.core.stats

import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.SideId
import kotlin.test.assertEquals
import org.junit.Test

class FirstPlayerStatsTest {
    @Test
    fun record_countsGamesWithAPickAndAWinner() {
        val games = listOf(
            ffa(ANA, BEN, winner = 0, first = 0, ended = 1), // first player won
            ffa(ANA, BEN, winner = 1, first = 0, ended = 2), // first player lost
            ffa(ANA, BEN, CLEO, winner = 2, first = 2, ended = 3), // won, 3 players
            ffa(ANA, BEN, winner = null, first = 0, ended = 4), // no winner: left out
            ffa(ANA, BEN, winner = 0, ended = 5), // no pick: left out
        )

        assertEquals(WinRecord(games = 3, wins = 2), firstPlayerRecord(games))
    }

    @Test
    fun record_teamWinCountsForTheFirstPlayer() {
        val games = listOf(
            teams(ANA to BEN, CLEO to DAN, winner = TEAM_A, first = 1, ended = 1),
            teams(ANA to BEN, CLEO to DAN, winner = TEAM_A, first = 2, ended = 2),
        )

        assertEquals(WinRecord(games = 2, wins = 1), firstPlayerRecord(games))
    }

    @Test
    fun record_ignoresPicksOfUnknownOrUnseatedPlayers_andUnknownWinners() {
        val base = ffa(ANA, BEN, CLEO, winner = 0, first = 2)
        val unseated = base.copy(config = base.config.copy(sides = base.config.sides.take(2)))
        val games = listOf(
            ffa(ANA, BEN, winner = 0).copy(firstPlayer = PlayerId("ghost")),
            unseated,
            ffa(ANA, BEN, first = 0).copy(winner = SideId("nobody")),
        )

        assertEquals(WinRecord(games = 0, wins = 0), firstPlayerRecord(games))
        assertEquals(0.0, firstPlayerRecord(games).winRate)
        assertEquals(WinRecord(0, 0), firstPlayerRecord(emptyList()))
    }

    @Test
    fun record_placeholderFirstPlayerStillCounts() {
        val game = ffa(Seat("Player 1", default = true), BEN, winner = 0, first = 0)

        assertEquals(WinRecord(games = 1, wins = 1), firstPlayerRecord(listOf(game)))
    }

    @Test
    fun split_goingFirstVsNot() {
        val games = listOf(
            ffa(ANA, BEN, winner = 0, first = 0, ended = 1), // Ana first, won
            ffa(ANA, BEN, winner = 1, first = 0, ended = 2), // Ana first, lost
            ffa(ANA, BEN, winner = 0, first = 1, ended = 3), // Ben first, Ana won
            ffa(ANA, BEN, winner = null, first = 0, ended = 4), // no winner: left out
            ffa(ANA, BEN, winner = 0, ended = 5), // no pick: left out
            ffa(BEN, CLEO, winner = 0, first = 0, ended = 6), // Ana didn't play
        )

        assertEquals(
            FirstPlayerSplit(goingFirst = WinRecord(2, 1), notFirst = WinRecord(1, 1)),
            firstPlayerSplit(games, key("Ana")),
        )
        assertEquals(0.5, firstPlayerSplit(games, key("Ana")).goingFirst.winRate)
    }

    @Test
    fun split_teammatesPickIsNotFirst_butTheTeamWinCounts() {
        val game = teams(ANA to BEN, CLEO to DAN, winner = TEAM_A, first = 1)

        assertEquals(
            FirstPlayerSplit(goingFirst = WinRecord(0, 0), notFirst = WinRecord(1, 1)),
            firstPlayerSplit(listOf(game), key("Ana")),
        )
        assertEquals(
            FirstPlayerSplit(goingFirst = WinRecord(1, 1), notFirst = WinRecord(0, 0)),
            firstPlayerSplit(listOf(game), key("Ben")),
        )
    }

    @Test
    fun split_bySavedPlayer_andWithDuplicateNamesInOneGame() {
        val games = listOf(
            ffa(Seat("Ana", preset = "p"), BEN, winner = 0, first = 0, ended = 1),
            ffa(Seat("Annie", preset = "p"), BEN, winner = 1, first = 1, ended = 2),
            // Two seats named "Ana" are one appearance; the second one was picked.
            ffa(ANA, Seat("ana"), winner = 1, first = 1, ended = 3),
        )

        assertEquals(
            FirstPlayerSplit(goingFirst = WinRecord(1, 1), notFirst = WinRecord(1, 0)),
            firstPlayerSplit(games, PlayerKey("preset:p")),
        )
        assertEquals(
            FirstPlayerSplit(goingFirst = WinRecord(1, 1), notFirst = WinRecord(0, 0)),
            firstPlayerSplit(games, key("Ana")),
        )
    }

    @Test
    fun split_placeholderPickIsNobody_andUnknownPlayerIsEmpty() {
        val game = ffa(ANA, Seat("Player 2", default = true), winner = 0, first = 1)

        assertEquals(
            FirstPlayerSplit(goingFirst = WinRecord(0, 0), notFirst = WinRecord(1, 1)),
            firstPlayerSplit(listOf(game), key("Ana")),
        )
        assertEquals(
            FirstPlayerSplit(WinRecord(0, 0), WinRecord(0, 0)),
            firstPlayerSplit(listOf(game), key("Player 2")),
        )
    }
}
