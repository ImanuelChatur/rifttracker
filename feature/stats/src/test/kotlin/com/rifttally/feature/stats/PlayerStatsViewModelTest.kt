package com.rifttally.feature.stats

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rifttally.core.model.Legend
import com.rifttally.core.stats.StatsPeriod
import com.rifttally.core.testing.MainDispatcherRule
import com.rifttally.core.testing.repository.FakeGameRepository
import com.rifttally.core.testing.repository.FakeLegendRepository
import com.rifttally.feature.stats.navigation.PlayerStatsRoute
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class PlayerStatsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val games = FakeGameRepository()
    private val legends = FakeLegendRepository(listOf(Legend("jinx", "Jinx", null)))

    private val day = 24 * 60 * 60 * 1000L
    private val now = 100 * day

    private fun viewModel(key: String, period: String? = null) = PlayerStatsViewModel(
        savedStateHandle = SavedStateHandle(
            buildMap {
                put(PlayerStatsRoute::playerKey.name, key)
                if (period != null) put(PlayerStatsRoute::period.name, period)
            },
        ),
        gameRepository = games,
        legendRepository = legends,
        gamesFilter = testGamesFilter(now),
        defaultDispatcher = mainDispatcherRule.testDispatcher,
    )

    @Test
    fun period_fromTheRoute_filtersGames_andIsKeptForOpponents() = runTest {
        games.addGame(duel("old", "Ana" to "jinx", "Ben" to null, winner = 0, endedAt = now - 40 * day))
        games.addGame(duel("new", "Ana" to null, "Cleo" to null, winner = 1, endedAt = now - day))

        val viewModel = viewModel("name:ana", period = StatsPeriod.LAST_30_DAYS.name)
        assertEquals(StatsPeriod.LAST_30_DAYS, viewModel.period)
        viewModel.uiState.test {
            val state = assertIs<PlayerStatsUiState.Success>(expectMostRecentItem())
            assertEquals(StatsPeriod.LAST_30_DAYS, state.period)
            assertEquals(1, state.games)
            assertEquals(0, state.wins)
            assertNull(state.favoriteLegend)
            assertEquals(listOf("Cleo"), state.headToHead.map { it.opponentName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun noGamesInPeriod_isNotFoundForThatPeriod() = runTest {
        games.addGame(duel("old", "Ana" to null, "Ben" to null, winner = 0, endedAt = now - 40 * day))

        viewModel("name:ana", period = StatsPeriod.LAST_30_DAYS.name).uiState.test {
            assertEquals(PlayerStatsUiState.NotFound(StatsPeriod.LAST_30_DAYS), expectMostRecentItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun missingOrUnknownPeriod_isAllTime() = runTest {
        assertEquals(StatsPeriod.ALL_TIME, viewModel("name:ana").period)
        assertEquals(StatsPeriod.ALL_TIME, viewModel("name:ana", period = "LAST_CENTURY").period)
    }

    @Test
    fun firstPlayer_winRateGoingFirstVsNot() = runTest {
        games.addGame(duel("g1", "Ana" to null, "Ben" to null, winner = 0, endedAt = 1, goesFirst = 0))
        games.addGame(duel("g2", "Ana" to null, "Ben" to null, winner = 1, endedAt = 2, goesFirst = 0))
        games.addGame(duel("g3", "Ana" to null, "Ben" to null, winner = 0, endedAt = 3, goesFirst = 1))

        viewModel("name:ana").uiState.test {
            assertEquals(
                FirstPlayerUiState(goingFirst = WinRateUiState(2, 50), notFirst = WinRateUiState(1, 100)),
                assertIs<PlayerStatsUiState.Success>(expectMostRecentItem()).firstPlayer,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun firstPlayer_onlyOneKind_orNone() = runTest {
        games.addGame(duel("g1", "Ana" to null, "Ben" to null, winner = 0, endedAt = 1, goesFirst = 1))
        games.addGame(duel("g2", "Cleo" to null, "Dan" to null, winner = 0, endedAt = 2))

        viewModel("name:ana").uiState.test {
            assertEquals(
                FirstPlayerUiState(goingFirst = null, notFirst = WinRateUiState(1, 100)),
                assertIs<PlayerStatsUiState.Success>(expectMostRecentItem()).firstPlayer,
            )
            cancelAndIgnoreRemainingEvents()
        }
        viewModel("name:cleo").uiState.test {
            assertNull(assertIs<PlayerStatsUiState.Success>(expectMostRecentItem()).firstPlayer)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun playerStats_withFavoriteLegendStreakAndHeadToHead() = runTest {
        games.addGame(duel("g1", "Ana" to "jinx", "Ben" to null, winner = 1, endedAt = 1))
        games.addGame(duel("g2", "Ana" to "jinx", "Ben" to null, winner = 0, endedAt = 2))
        games.addGame(duel("g3", "Ana" to null, "Cleo" to null, winner = 0, endedAt = 3))
        games.addGame(duel("g4", "Ben" to null, "Cleo" to null, winner = null, endedAt = 4))

        viewModel("name:ana").uiState.test {
            val state = assertIs<PlayerStatsUiState.Success>(expectMostRecentItem())
            assertEquals("Ana", state.name)
            assertEquals(3, state.games)
            assertEquals(2, state.wins)
            assertEquals(67, state.winRatePercent)
            assertEquals(2, state.currentWinStreak)
            assertEquals("Jinx", state.favoriteLegend?.name)
            assertEquals(
                listOf(
                    HeadToHeadUiState("name:ben", "Ben", games = 2, wins = 1, opponentWins = 1),
                    HeadToHeadUiState("name:cleo", "Cleo", games = 1, wins = 1, opponentWins = 0),
                ),
                state.headToHead,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun presetKey_groupsGamesUnderDifferentNames() = runTest {
        games.addGame(duel("g1", "Ana" to null, "Ben" to null, winner = 0, endedAt = 1, firstPreset = "p"))
        games.addGame(duel("g2", "Annie" to null, "Ben" to null, winner = 0, endedAt = 2, firstPreset = "p"))

        viewModel("preset:p").uiState.test {
            val state = assertIs<PlayerStatsUiState.Success>(expectMostRecentItem())
            assertEquals("Annie", state.name)
            assertEquals(2, state.games)
            assertNull(state.favoriteLegend)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun unknownPlayer_isNotFound() = runTest {
        games.addGame(duel("g1", "Ana" to null, "Ben" to null, winner = 0, endedAt = 1))

        viewModel("name:zed").uiState.test {
            assertIs<PlayerStatsUiState.NotFound>(expectMostRecentItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
