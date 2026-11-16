package com.rifttally.feature.stats

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rifttally.core.model.Legend
import com.rifttally.core.stats.StatsPeriod
import com.rifttally.core.testing.MainDispatcherRule
import com.rifttally.core.testing.repository.FakeGameRepository
import com.rifttally.core.testing.repository.FakeLegendRepository
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class StatsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val games = FakeGameRepository()
    private val legends = FakeLegendRepository(
        listOf(
            Legend("jinx", "Jinx", "file:///jinx.webp"),
            Legend("viktor", "Viktor", null),
            Legend("ahri", "Ahri", null),
        ),
    )

    /** 2026-11-15 12:00 in Berlin. */
    private val now = ZonedDateTime.of(2026, 11, 15, 12, 0, 0, 0, TEST_ZONE).toInstant().toEpochMilli()
    private val day = 24 * 60 * 60 * 1000L

    // Lazy: created inside each test, after MainDispatcherRule has installed the test Main dispatcher.
    private val savedState = SavedStateHandle()
    private val viewModel by lazy { newViewModel() }

    private fun newViewModel() =
        StatsViewModel(savedState, games, legends, testGamesFilter(now), mainDispatcherRule.testDispatcher)

    private fun addSampleGames() {
        // Ana (Jinx) 3 wins of 4; Ben (Viktor) 1 of 3; Cleo (Ahri) 0 of 1, plus a no-winner game.
        games.addGame(duel("g1", "Ana" to "jinx", "Ben" to "viktor", winner = 0, endedAt = 1))
        games.addGame(duel("g2", "Ana" to "jinx", "Ben" to "viktor", winner = 0, endedAt = 2))
        games.addGame(duel("g3", "Ana" to "jinx", "Ben" to "viktor", winner = 1, endedAt = 3))
        games.addGame(duel("g4", "Ana" to "jinx", "Cleo" to "ahri", winner = 0, endedAt = 4))
        games.addGame(duel("g5", "Ben" to null, "Cleo" to null, winner = null, endedAt = 5))
    }

    @Test
    fun noGames_isEmpty() = runTest {
        games.createGame(duel("x", "Ana" to null, "Ben" to null, null, 0).config, emptyList()) // active only

        viewModel.uiState.test {
            assertIs<StatsUiState.Empty>(expectMostRecentItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun overview_totalsMostPlayedLegendAndTopPlayers() = runTest {
        addSampleGames()

        viewModel.uiState.test {
            val state = assertIs<StatsUiState.Success>(expectMostRecentItem())
            assertEquals(5, state.totalGames)
            assertEquals("Jinx", state.mostPlayedLegend?.name)
            assertEquals("file:///jinx.webp", state.mostPlayedLegend?.imageUri)
            assertEquals(4, state.mostPlayedLegend?.games)
            // Ana 3/4 and Ben 1/4 have enough games; Cleo (2 games) doesn't.
            assertEquals(listOf("Ana" to 75, "Ben" to 25), state.topPlayers.map { it.name to it.winRatePercent })
            assertEquals(listOf("Ana", "Ben", "Cleo"), state.players.map { it.name })
            assertEquals("name:ana", state.players.first().key)
            assertEquals(3, state.minGamesForRanking)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun legendTab_sortsByGamesWinRateOrName() = runTest {
        addSampleGames()

        viewModel.uiState.test {
            var state = assertIs<StatsUiState.Success>(expectMostRecentItem())
            assertEquals(LegendSort.GAMES, state.legendSort)
            assertEquals(listOf("Jinx", "Viktor", "Ahri"), state.legends.map { it.name })

            viewModel.onLegendSortSelect(LegendSort.WIN_RATE)
            state = assertIs<StatsUiState.Success>(expectMostRecentItem())
            assertEquals(
                listOf("Jinx" to 75, "Viktor" to 33, "Ahri" to 0),
                state.legends.map {
                    it.name to
                        it.winRatePercent
                },
            )

            viewModel.onLegendSortSelect(LegendSort.NAME)
            state = assertIs<StatsUiState.Success>(expectMostRecentItem())
            assertEquals(listOf("Ahri", "Jinx", "Viktor"), state.legends.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun tabSelection_isKept() = runTest {
        addSampleGames()

        viewModel.uiState.test {
            viewModel.onTabSelect(StatsTab.LEGENDS)
            assertEquals(StatsTab.LEGENDS, assertIs<StatsUiState.Success>(expectMostRecentItem()).selectedTab)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun period_filtersEverythingByEndDate() = runTest {
        val newYear = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, TEST_ZONE).toInstant().toEpochMilli()
        games.addGame(duel("old", "Ana" to "jinx", "Ben" to null, winner = 0, endedAt = newYear - 1))
        games.addGame(duel("spring", "Ana" to null, "Cleo" to "ahri", winner = 1, endedAt = newYear + day))
        games.addGame(duel("recent", "Ben" to "viktor", "Cleo" to null, winner = 0, endedAt = now - day))

        viewModel.uiState.test {
            var state = assertIs<StatsUiState.Success>(expectMostRecentItem())
            assertEquals(StatsPeriod.ALL_TIME, state.period)
            assertEquals(3, state.totalGames)

            viewModel.onPeriodSelect(StatsPeriod.THIS_YEAR)
            state = assertIs<StatsUiState.Success>(expectMostRecentItem())
            assertEquals(StatsPeriod.THIS_YEAR, state.period)
            assertEquals(2, state.totalGames)
            assertEquals(listOf("Ahri", "Viktor"), state.legends.map { it.name })

            viewModel.onPeriodSelect(StatsPeriod.LAST_30_DAYS)
            state = assertIs<StatsUiState.Success>(expectMostRecentItem())
            assertEquals(1, state.totalGames)
            assertEquals(listOf("Ben", "Cleo"), state.players.map { it.name })
            assertEquals("Viktor", state.mostPlayedLegend?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun periodTabAndSort_surviveRecreation() = runTest {
        games.addGame(duel("recent", "Ana" to "jinx", "Ben" to "viktor", winner = 0, endedAt = now - day))
        viewModel.onPeriodSelect(StatsPeriod.THIS_YEAR)
        viewModel.onTabSelect(StatsTab.LEGENDS)
        viewModel.onLegendSortSelect(LegendSort.NAME)

        // A new ViewModel on the same saved state, as after process death.
        newViewModel().uiState.test {
            val state = assertIs<StatsUiState.Success>(expectMostRecentItem())
            assertEquals(StatsPeriod.THIS_YEAR, state.period)
            assertEquals(StatsTab.LEGENDS, state.selectedTab)
            assertEquals(LegendSort.NAME, state.legendSort)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyPeriod_keepsTheChips_andAllTimeComesBack() = runTest {
        games.addGame(duel("old", "Ana" to null, "Ben" to null, winner = 0, endedAt = now - 40 * day))

        viewModel.uiState.test {
            viewModel.onPeriodSelect(StatsPeriod.LAST_30_DAYS)
            assertEquals(StatsUiState.EmptyPeriod(StatsPeriod.LAST_30_DAYS), expectMostRecentItem())

            viewModel.onPeriodSelect(StatsPeriod.ALL_TIME)
            assertEquals(1, assertIs<StatsUiState.Success>(expectMostRecentItem()).totalGames)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun firstPlayerRate_overGamesWithAPickAndAWinner() = runTest {
        games.addGame(duel("g1", "Ana" to null, "Ben" to null, winner = 0, endedAt = 1, goesFirst = 0))
        games.addGame(duel("g2", "Ana" to null, "Ben" to null, winner = 0, endedAt = 2, goesFirst = 1))
        games.addGame(duel("g3", "Ana" to null, "Ben" to null, winner = 1, endedAt = 3, goesFirst = 1))
        games.addGame(duel("g4", "Ana" to null, "Ben" to null, winner = null, endedAt = 4, goesFirst = 0))
        games.addGame(duel("g5", "Ana" to null, "Ben" to null, winner = 0, endedAt = 5))

        viewModel.uiState.test {
            assertEquals(
                WinRateUiState(games = 3, winRatePercent = 67),
                assertIs<StatsUiState.Success>(expectMostRecentItem()).firstPlayer,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun noPicks_hasNoFirstPlayerRate() = runTest {
        addSampleGames()

        viewModel.uiState.test {
            assertNull(assertIs<StatsUiState.Success>(expectMostRecentItem()).firstPlayer)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun unknownLegendId_fallsBackToTheId() = runTest {
        games.addGame(duel("g", "Ana" to "zed", "Ben" to null, winner = 0, endedAt = 1))

        viewModel.uiState.test {
            val state = assertIs<StatsUiState.Success>(expectMostRecentItem())
            assertEquals("zed", state.mostPlayedLegend?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
