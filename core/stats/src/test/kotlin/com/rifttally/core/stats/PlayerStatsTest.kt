package com.rifttally.core.stats

import com.rifttally.core.model.Player
import com.rifttally.core.model.PlayerId
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Test

class PlayerStatsTest {
    @Test
    fun key_usesPresetId_elseTrimmedLowercaseName() {
        val byName = PlayerKey.of(Player(PlayerId("p1"), "  Ana ", "orange"))
        assertEquals(PlayerKey.of(Player(PlayerId("p2"), "ANA", "sky")), byName)
        val byPreset = PlayerKey.of(Player(PlayerId("p1"), "Ana", "orange", presetId = "x"))
        assertNotEquals(byName, byPreset)
        assertEquals(byPreset, PlayerKey.of(Player(PlayerId("p9"), "Renamed", "sky", presetId = "x")))
    }

    @Test
    fun gamesAndWins_includeNoWinnerGamesAsPlayed() {
        val games = listOf(
            ffa(ANA, BEN, winner = 0, ended = 1),
            ffa(ANA, BEN, winner = 1, ended = 2),
            ffa(ANA, BEN, winner = null, ended = 3),
            ffa(ANA, BEN, winner = 0, ended = 4),
        )

        val ana = assertNotNull(playerStats(games, key("ana")))

        assertEquals(4, ana.games)
        assertEquals(2, ana.wins)
        assertEquals(0.5, ana.winRate)
        assertEquals(1, ana.currentWinStreak)
    }

    @Test
    fun currentStreak_countsBackFromNewest_regardlessOfInputOrder() {
        val games = listOf(
            ffa(ANA, BEN, winner = 0, ended = 40),
            ffa(ANA, BEN, winner = 1, ended = 10),
            ffa(ANA, BEN, winner = 0, ended = 30),
            ffa(ANA, BEN, winner = 0, ended = 20),
        )

        assertEquals(3, playerStats(games, key("ana"))?.currentWinStreak)
        assertEquals(0, playerStats(games, key("ben"))?.currentWinStreak)
    }

    @Test
    fun noWinnerGame_breaksTheStreak() {
        val games = listOf(ffa(ANA, BEN, winner = 0, ended = 1), ffa(ANA, BEN, winner = null, ended = 2))
        assertEquals(0, playerStats(games, key("ana"))?.currentWinStreak)
    }

    @Test
    fun groupsByName_caseAndSpaceInsensitive_showingTheLatestName() {
        val games = listOf(ffa(Seat("ana "), BEN, ended = 1), ffa(Seat("Ana"), BEN, ended = 2))

        val ana = assertNotNull(playerStats(games, key("ana")))

        assertEquals(2, ana.games)
        assertEquals("Ana", ana.name)
    }

    @Test
    fun groupsByPreset_evenWhenTheNameChanged_andSeparatesFromSameNameWithoutPreset() {
        val games = listOf(
            ffa(Seat("Ana", preset = "a"), BEN, winner = 0, ended = 1),
            ffa(Seat("Annie", preset = "a"), BEN, winner = 0, ended = 2),
            ffa(Seat("Ana"), BEN, ended = 3),
        )

        val all = playerStats(games)

        val preset = all.single { it.key == PlayerKey("preset:a") }
        assertEquals(2, preset.games)
        assertEquals("Annie", preset.name)
        assertEquals(1, all.single { it.key == key("ana") }.games)
    }

    @Test
    fun favoriteLegend_isMostPicked_tiesGoToTheMoreRecent_noneIsNull() {
        val games = listOf(
            ffa(Seat("Ana", legend = "jinx"), BEN, ended = 1),
            ffa(Seat("Ana", legend = "viktor"), BEN, ended = 2),
            ffa(Seat("Ana", legend = "jinx"), BEN, ended = 3),
            ffa(Seat("Ana", legend = "viktor"), BEN, ended = 4),
            ffa(Seat("Ana"), BEN, ended = 5),
        )

        assertEquals("viktor", playerStats(games, key("ana"))?.favoriteLegendId)
        assertNull(playerStats(games, key("ben"))?.favoriteLegendId)
        val moreJinx = games + ffa(Seat("Ana", legend = "jinx"), BEN, ended = 0)
        assertEquals("jinx", playerStats(moreJinx, key("ana"))?.favoriteLegendId)
    }

    @Test
    fun teamWin_countsForBothTeammates() {
        val games = listOf(teams(ANA to BEN, CLEO to DAN, winner = TEAM_A))

        val all = playerStats(games).associateBy { it.name }

        assertEquals(listOf(1, 1, 0, 0), listOf("Ana", "Ben", "Cleo", "Dan").map { all.getValue(it).wins })
        assertEquals(listOf(1, 1, 1, 1), listOf("Ana", "Ben", "Cleo", "Dan").map { all.getValue(it).games })
    }

    @Test
    fun sameKeyTwiceInOneGame_countsOnce() {
        val games = listOf(ffa(ANA, Seat("ana"), winner = 1))

        val ana = assertNotNull(playerStats(games, key("ana")))

        assertEquals(1, ana.games)
        assertEquals(1, ana.wins)
    }

    @Test
    fun list_sortedByGamesThenName_unknownKeyIsNull() {
        val games = listOf(ffa(BEN, CLEO, ended = 1), ffa(ANA, CLEO, ended = 2), ffa(ANA, Seat("Dan"), ended = 3))

        assertEquals(listOf("Ana", "Cleo", "Ben", "Dan"), playerStats(games).map { it.name })
        assertNull(playerStats(games, key("zed")))
        assertEquals(emptyList(), playerStats(emptyList()))
    }

    @Test
    fun winRate_isZeroWithoutGames() {
        assertEquals(0.0, PlayerStats(key("a"), "A", 0, 0, 0, null).winRate)
    }

    @Test
    fun topPlayers_needThreeGames_sortedByWinRate_tiesByWinsThenName() {
        val eve = Seat("Eve")
        val games = listOf(
            ffa(ANA, BEN, winner = 0, ended = 1),
            ffa(ANA, BEN, winner = 0, ended = 2),
            ffa(ANA, BEN, winner = 1, ended = 3),
            ffa(CLEO, DAN, winner = 0, ended = 4),
            ffa(CLEO, DAN, winner = 1, ended = 5),
            ffa(CLEO, DAN, winner = 1, ended = 6),
            ffa(CLEO, eve, winner = 0, ended = 7),
            ffa(CLEO, eve, winner = 0, ended = 8),
        )
        // Ana 2/3, Dan 2/3, Cleo 3/5, Ben 1/3; Eve has only 2 games.

        val top = topPlayersByWinRate(games)

        assertEquals(listOf("Ana", "Dan", "Cleo", "Ben"), top.map { it.name })
        assertEquals("Eve", topPlayersByWinRate(games, minGames = 1).last().name)
        assertEquals(emptyList(), topPlayersByWinRate(games.take(2)))
    }

    @Test
    fun topPlayers_equalRate_moreWinsFirst() {
        val games = listOf(
            ffa(ANA, BEN, winner = 0, ended = 1),
            ffa(ANA, BEN, winner = 1, ended = 2),
            ffa(ANA, BEN, winner = null, ended = 3),
            ffa(CLEO, DAN, winner = 0, ended = 4),
            ffa(CLEO, DAN, winner = 0, ended = 5),
            ffa(CLEO, DAN, winner = 1, ended = 6),
            ffa(CLEO, DAN, winner = 1, ended = 7),
            ffa(CLEO, DAN, winner = null, ended = 8),
            ffa(CLEO, DAN, winner = null, ended = 9),
        )
        // Ana, Ben 1/3; Cleo, Dan 2/6: same rate, Cleo and Dan have more wins.

        assertEquals(listOf("Cleo", "Dan", "Ana", "Ben"), topPlayersByWinRate(games).map { it.name })
    }
}
