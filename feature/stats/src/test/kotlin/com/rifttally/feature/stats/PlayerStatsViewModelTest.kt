package com.rifttally.feature.stats

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rifttally.core.model.Legend
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

    private fun viewModel(key: String) = PlayerStatsViewModel(
        savedStateHandle = SavedStateHandle(mapOf(PlayerStatsRoute::playerKey.name to key)),
        gameRepository = games,
        legendRepository = legends,
    )

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
