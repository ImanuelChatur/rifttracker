package com.rifttally.feature.home

import app.cash.turbine.test
import com.rifttally.core.game.GameEvent
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.Legend
import com.rifttally.core.model.Player
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.Side
import com.rifttally.core.model.SideId
import com.rifttally.core.testing.MainDispatcherRule
import com.rifttally.core.testing.TestClock
import com.rifttally.core.testing.repository.FakeGameRepository
import com.rifttally.core.testing.repository.FakeLegendRepository
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTime::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val games = FakeGameRepository()
    private val legends = FakeLegendRepository(listOf(Legend("jinx", "Jinx", null)))

    // Lazy: created inside each test, after MainDispatcherRule has installed the test Main dispatcher.
    private val viewModel by lazy { HomeViewModel(games, legends, TestClock(Instant.fromEpochMilliseconds(5_000))) }

    private val config = GameConfig(
        format = GameFormat.TWO_PLAYERS,
        sides = listOf(Side(SideId("s1"), listOf(PlayerId("p1"))), Side(SideId("s2"), listOf(PlayerId("p2")))),
        players = listOf(Player(PlayerId("p1"), "Ana", "orange", "jinx"), Player(PlayerId("p2"), "Ben", "sky")),
    )

    @Test
    fun noGames_noResumeCard() = runTest {
        viewModel.uiState.test {
            assertNull(assertIs<HomeUiState.Success>(expectMostRecentItem()).activeGame)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun activeGame_showsScoresAndLegends() = runTest {
        val id = games.createGame(config)
        games.appendEvent(id, GameEvent.PointsAdjusted("e1", 0, SideId("s2"), 4))

        viewModel.uiState.test {
            val game = assertNotNull(assertIs<HomeUiState.Success>(expectMostRecentItem()).activeGame)
            assertEquals(id, game.gameId)
            assertEquals(listOf("Ana", "Ben"), game.sides.map { it.name })
            assertEquals(listOf(0, 4), game.sides.map { it.points })
            assertEquals("Jinx", game.sides[0].legendName)
            assertEquals(8, game.targetScore)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun endedGame_isNotResumable() = runTest {
        val id = games.createGame(config)
        games.appendEvent(id, GameEvent.GameEnded("e1", 0, null))

        viewModel.uiState.test {
            assertNull(assertIs<HomeUiState.Success>(expectMostRecentItem()).activeGame)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun endedGames_areListedAsRecent_withWinner() = runTest {
        val id = games.createGame(config)
        games.appendEvent(id, GameEvent.PointsAdjusted("e1", 0, SideId("s1"), 8))
        games.appendEvent(id, GameEvent.GameEnded("e2", 1_000, SideId("s1")))

        viewModel.uiState.test {
            val state = assertIs<HomeUiState.Success>(expectMostRecentItem())
            val recent = state.recentGames.single()
            assertEquals(id, recent.gameId)
            assertEquals(1_000L, recent.endedAt)
            assertEquals("Ana", recent.winnerName)
            assertEquals(listOf(true, false), recent.sides.map { it.isWinner })
            assertEquals(listOf(8, 0), recent.sides.map { it.points })
            assertEquals(5_000L, state.now)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sameNames_onlyTheWinningSideIsMarked() = runTest {
        val twins = config.copy(players = config.players.map { it.copy(name = "Ana") })
        val id = games.createGame(twins)
        games.appendEvent(id, GameEvent.GameEnded("e1", 1_000, SideId("s2")))

        viewModel.uiState.test {
            val recent = assertIs<HomeUiState.Success>(expectMostRecentItem()).recentGames.single()
            assertEquals(listOf(false, true), recent.sides.map { it.isWinner })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
