package com.rifttally.feature.stats

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rifttally.core.model.Legend
import com.rifttally.core.stats.StatsPeriod
import com.rifttally.core.testing.MainDispatcherRule
import com.rifttally.core.testing.repository.FakeGameRepository
import com.rifttally.core.testing.repository.FakeLegendRepository
import com.rifttally.feature.stats.navigation.ALL_TIME
import com.rifttally.feature.stats.navigation.LegendStatsRoute
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class LegendStatsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val games = FakeGameRepository()
    private val legends = FakeLegendRepository(
        listOf(Legend("jinx", "Jinx", "file:///jinx.webp"), Legend("viktor", "Viktor", null)),
    )
    private val day = 24 * 60 * 60 * 1000L
    private val now = 100 * day

    private fun viewModel(legendId: String, period: StatsPeriod = StatsPeriod.ALL_TIME) = LegendStatsViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(LegendStatsRoute::legendId.name to legendId, LegendStatsRoute::period.name to period.name),
        ),
        gameRepository = games,
        legendRepository = legends,
        gamesFilter = testGamesFilter(now),
        defaultDispatcher = mainDispatcherRule.testDispatcher,
    )

    @Test
    fun legend_withGamesWinRateAndMatchups() = runTest {
        games.addGame(duel("g1", "Ana" to "jinx", "Ben" to "viktor", winner = 0, endedAt = 1))
        games.addGame(duel("g2", "Ana" to "jinx", "Ben" to "viktor", winner = 1, endedAt = 2))
        games.addGame(duel("g3", "Ana" to "jinx", "Ben" to "viktor", winner = 0, endedAt = 3))
        games.addGame(duel("g4", "Ana" to "jinx", "Cleo" to "zed", winner = null, endedAt = 4))
        // Mirror match: counts for the Legend's games, not as a matchup.
        games.addGame(duel("g5", "Ana" to "jinx", "Ben" to "jinx", winner = 0, endedAt = 5))

        viewModel("jinx").uiState.test {
            val state = assertIs<LegendStatsUiState.Success>(expectMostRecentItem())
            assertEquals(
                LegendRowUiState("jinx", "Jinx", "file:///jinx.webp", games = 6, winRatePercent = 50),
                state.legend,
            )
            assertEquals(3, state.wins)
            assertEquals(
                listOf(
                    MatchupUiState(LegendRowUiState("viktor", "Viktor", null, 3, 67), wins = 2, losses = 1),
                    // Unknown Legend: the id is the name.
                    MatchupUiState(LegendRowUiState("zed", "zed", null, 1, 0), wins = 0, losses = 0),
                ),
                state.matchups,
            )
            assertEquals(StatsPeriod.ALL_TIME, state.period)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun period_filtersGames() = runTest {
        games.addGame(duel("old", "Ana" to "jinx", "Ben" to "viktor", winner = 0, endedAt = now - 40 * day))
        games.addGame(duel("new", "Ana" to "jinx", "Ben" to null, winner = 1, endedAt = now - day))

        viewModel("jinx", StatsPeriod.LAST_30_DAYS).uiState.test {
            val state = assertIs<LegendStatsUiState.Success>(expectMostRecentItem())
            assertEquals(1, state.legend.games)
            assertEquals(0, state.wins)
            assertEquals(emptyList(), state.matchups)
            assertEquals(StatsPeriod.LAST_30_DAYS, state.period)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun legendNotPlayedInPeriod_isNotFound() = runTest {
        games.addGame(duel("old", "Ana" to "jinx", "Ben" to null, winner = 0, endedAt = now - 40 * day))

        viewModel("jinx", StatsPeriod.LAST_30_DAYS).uiState.test {
            assertEquals(LegendStatsUiState.NotFound(StatsPeriod.LAST_30_DAYS), expectMostRecentItem())
            cancelAndIgnoreRemainingEvents()
        }
        viewModel("ahri").uiState.test {
            assertEquals(LegendStatsUiState.NotFound(StatsPeriod.ALL_TIME), expectMostRecentItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun routeDefault_isAllTime() {
        assertEquals(StatsPeriod.ALL_TIME.name, ALL_TIME)
        assertEquals(ALL_TIME, LegendStatsRoute("jinx").period)
    }
}
