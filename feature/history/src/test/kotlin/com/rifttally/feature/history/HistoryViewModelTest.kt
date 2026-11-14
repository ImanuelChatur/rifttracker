package com.rifttally.feature.history

import app.cash.turbine.test
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
import com.rifttally.core.model.Legend
import com.rifttally.core.testing.MainDispatcherRule
import com.rifttally.core.testing.repository.FakeGameRepository
import com.rifttally.core.testing.repository.FakeLegendRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val games = FakeGameRepository()
    private val legends = FakeLegendRepository(
        listOf(Legend("jinx", "Jinx", "file:///jinx.webp"), Legend("viktor", "Viktor", null)),
    )

    // Lazy: created inside each test, after MainDispatcherRule has installed the test Main dispatcher.
    private val viewModel by lazy { HistoryViewModel(games, legends) }

    @Test
    fun noEndedGames_isEmptyHistory() = runTest {
        games.createGame(twoPlayers) // still active

        viewModel.uiState.test {
            val state = assertIs<HistoryUiState.Success>(expectMostRecentItem())
            assertFalse(state.hasAnyGames)
            assertTrue(state.games.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun endedGames_newestFirst_withScoresLegendsAndWinner() = runTest {
        games.addGame(endedGame("old", endedAt = 100, points = listOf(S1 to 3, S2 to 1, S1 to 5)))
        games.addGame(endedGame("new", endedAt = 200, config = teams, winner = TEAM_B, points = listOf(TEAM_B to 11)))
        games.addGame(endedGame("draw", endedAt = 150, winner = null))

        viewModel.uiState.test {
            val state = assertIs<HistoryUiState.Success>(expectMostRecentItem())
            assertEquals(listOf("new", "draw", "old"), state.games.map { it.gameId.value })

            val old = state.games[2]
            assertEquals(100, old.endedAt)
            assertEquals(GameFormat.TWO_PLAYERS, old.format)
            assertEquals(listOf(8, 1), old.sides.map { it.score })
            assertEquals(listOf("Jinx", null), old.sides.map { it.legendNames })
            assertEquals("file:///jinx.webp", old.sides[0].legendImageUri)
            assertEquals(listOf(true, false), old.sides.map { it.isWinner })
            assertEquals("Ana", old.winnerName)

            val team = state.games[0]
            assertEquals(listOf("Ana & Cleo", "Ben & Dan"), team.sides.map { it.name })
            assertEquals("Jinx & Viktor", team.sides[0].legendNames)
            assertEquals("Ben & Dan", team.winnerName)

            assertNull(state.games[1].winnerName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun formatFilter_showsOnlyThatFormat_andNullShowsAll() = runTest {
        games.addGame(endedGame("duel", endedAt = 100))
        games.addGame(endedGame("team", endedAt = 200, config = teams, winner = TEAM_A))

        viewModel.uiState.test {
            viewModel.onFormatSelect(GameFormat.TEAMS_2V2)
            var state = assertIs<HistoryUiState.Success>(expectMostRecentItem())
            assertEquals(listOf("team"), state.games.map { it.gameId.value })
            assertEquals(GameFormat.TEAMS_2V2, state.selectedFormat)

            viewModel.onFormatSelect(GameFormat.THREE_PLAYERS)
            state = assertIs<HistoryUiState.Success>(expectMostRecentItem())
            assertTrue(state.games.isEmpty())
            assertTrue(state.hasAnyGames)

            viewModel.onFormatSelect(null)
            state = assertIs<HistoryUiState.Success>(expectMostRecentItem())
            assertEquals(2, state.games.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun delete_removesGame_andUndoRestoresItExactly() = runTest {
        val record = endedGame("g", endedAt = 100, points = listOf(S1 to 2))
        games.addGame(record)

        viewModel.uiState.test {
            viewModel.onDeleteGame(GameId("g"))
            var state = assertIs<HistoryUiState.Success>(expectMostRecentItem())
            assertTrue(state.games.isEmpty())
            assertEquals(GameId("g"), state.recentlyDeleted)
            assertTrue(games.allGames.isEmpty())

            viewModel.onUndoDelete()
            state = assertIs<HistoryUiState.Success>(expectMostRecentItem())
            assertEquals(listOf("g"), state.games.map { it.gameId.value })
            assertNull(state.recentlyDeleted)
            assertEquals(listOf(record), games.allGames)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun delete_thenDismiss_staysDeleted_andUndoDoesNothing() = runTest {
        games.addGame(endedGame("g", endedAt = 100))

        viewModel.uiState.test {
            viewModel.onDeleteGame(GameId("g"))
            viewModel.onUndoDismiss()
            viewModel.onUndoDelete()
            val state = assertIs<HistoryUiState.Success>(expectMostRecentItem())
            assertNull(state.recentlyDeleted)
            assertFalse(state.hasAnyGames)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deletingUnknownGame_doesNothing() = runTest {
        games.addGame(endedGame("g", endedAt = 100))

        viewModel.uiState.test {
            viewModel.onDeleteGame(GameId("missing"))
            val state = assertIs<HistoryUiState.Success>(expectMostRecentItem())
            assertNull(state.recentlyDeleted)
            assertEquals(1, state.games.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
