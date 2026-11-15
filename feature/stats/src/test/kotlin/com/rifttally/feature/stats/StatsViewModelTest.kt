package com.rifttally.feature.stats

import app.cash.turbine.test
import com.rifttally.core.model.Legend
import com.rifttally.core.testing.MainDispatcherRule
import com.rifttally.core.testing.repository.FakeGameRepository
import com.rifttally.core.testing.repository.FakeLegendRepository
import kotlin.test.assertEquals
import kotlin.test.assertIs
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

    // Lazy: created inside each test, after MainDispatcherRule has installed the test Main dispatcher.
    private val viewModel by lazy { StatsViewModel(games, legends, mainDispatcherRule.testDispatcher) }

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
    fun unknownLegendId_fallsBackToTheId() = runTest {
        games.addGame(duel("g", "Ana" to "zed", "Ben" to null, winner = 0, endedAt = 1))

        viewModel.uiState.test {
            val state = assertIs<StatsUiState.Success>(expectMostRecentItem())
            assertEquals("zed", state.mostPlayedLegend?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
