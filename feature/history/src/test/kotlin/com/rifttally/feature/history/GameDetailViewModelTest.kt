package com.rifttally.feature.history

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rifttally.core.data.game.FirstPlayerPicker
import com.rifttally.core.game.GameEvent
import com.rifttally.core.game.GameEventFactory
import com.rifttally.core.game.GameLogLine
import com.rifttally.core.game.IdGenerator
import com.rifttally.core.model.Legend
import com.rifttally.core.testing.MainDispatcherRule
import com.rifttally.core.testing.TestClock
import com.rifttally.core.testing.repository.FakeGameRepository
import com.rifttally.core.testing.repository.FakeLegendRepository
import com.rifttally.core.testing.repository.FakeUserSettingsRepository
import com.rifttally.feature.history.navigation.GameDetailRoute
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTime::class)
class GameDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val games = FakeGameRepository()
    private val legends = FakeLegendRepository(listOf(Legend("jinx", "Jinx", null)))
    private val settings = FakeUserSettingsRepository()

    private fun viewModel(gameId: String) = GameDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(GameDetailRoute::gameId.name to gameId)),
        gameRepository = games,
        legendRepository = legends,
        defaultDispatcher = mainDispatcherRule.testDispatcher,
        firstPlayerPicker = FirstPlayerPicker(settings, { 0 }, GameEventFactory(TestClock(), IdGenerator { "pick" })),
        shareDispatcher = mainDispatcherRule.testDispatcher,
    )

    @Test
    fun endedGame_showsScoreboardDurationChartAndLog() = runTest {
        games.addGame(
            endedGame(
                id = "g",
                createdAt = 0,
                endedAt = 25 * 60_000L + 30_000,
                points = listOf(S1 to 3, S2 to 2, S1 to -1, S1 to 6),
            ),
        )

        viewModel("g").uiState.test {
            val state = assertIs<GameDetailUiState.Success>(expectMostRecentItem())
            assertEquals(25, state.durationMinutes)
            assertEquals(8, state.targetScore)
            assertEquals(listOf(8, 2), state.sides.map { it.score })
            assertEquals("Ana", state.winnerName)
            assertEquals(listOf("Ana", "Ben"), state.chart.map { it.name })
            assertEquals(listOf("orange", "sky"), state.chart.map { it.colorId })
            assertEquals(listOf(0, 3, 3, 2, 8), state.chart[0].points)
            assertEquals(listOf(0, 0, 2, 2, 2), state.chart[1].points)
            assertEquals(
                listOf(
                    GameLogLine.Points("g-e0", "Ana", 3, 3),
                    GameLogLine.Points("g-e1", "Ben", 2, 2),
                    GameLogLine.Points("g-e2", "Ana", -1, 2),
                    GameLogLine.Points("g-e3", "Ana", 6, 8),
                    GameLogLine.GameEnded("g-end", "Ana"),
                ),
                state.log,
            )
            val card = state.resultCard
            assertEquals(listOf(8, 2), card.sides.map { it.score })
            assertEquals("Ana", card.winner?.name)
            assertEquals(25 * 60_000L + 30_000, card.endedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun teamGame_usesTeamNames() = runTest {
        games.addGame(endedGame("t", endedAt = 1_000, config = teams, winner = TEAM_B, points = listOf(TEAM_B to 1)))

        viewModel("t").uiState.test {
            val state = assertIs<GameDetailUiState.Success>(expectMostRecentItem())
            assertEquals("Ben & Dan", state.winnerName)
            assertEquals(GameLogLine.Points("t-e0", "Ben & Dan", 1, 1), state.log.first())
            assertEquals(11, state.targetScore)
            // The share card has every teammate with their own color, and the winning team.
            assertEquals(listOf("Ana & Cleo", "Ben & Dan"), state.resultCard.sides.map { it.name })
            assertEquals("Ben & Dan", state.resultCard.winner?.name)
            assertEquals(listOf(0, 1), state.resultCard.sides.map { it.score })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun playAgain_createsTheSameLineupWithAPick_andOpensIt() = runTest {
        val played = endedGame("g", endedAt = 1_000, points = listOf(S1 to 2))
        // The target was raised mid-game: Play again starts from the target the game ended on.
        val ended = played.copy(events = listOf(GameEvent.TargetScoreChanged("t", 1, 12)) + played.events)
        games.addGame(ended)
        val viewModel = viewModel("g")
        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.navigationEvents.test {
                viewModel.onPlayAgain()
                viewModel.onPlayAgain() // double tap: one game
                val event = assertIs<GameDetailNavigationEvent.OpenTracker>(awaitItem())
                val created = games.allGames.first { it.id == event.gameId }
                assertEquals(ended.config.copy(targetScore = 12), created.config)
                assertIs<GameEvent.FirstPlayerChosen>(created.events.single())
            }
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(2, games.allGames.size)
    }

    @Test
    fun missingGame_isNotFound() = runTest {
        viewModel("missing").uiState.test {
            assertIs<GameDetailUiState.NotFound>(expectMostRecentItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
