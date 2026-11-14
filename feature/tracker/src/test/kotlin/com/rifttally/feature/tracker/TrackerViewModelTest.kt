package com.rifttally.feature.tracker

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rifttally.core.game.GameEvent
import com.rifttally.core.game.GameEventFactory
import com.rifttally.core.game.IdGenerator
import com.rifttally.core.model.GameConfig
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
import com.rifttally.core.model.Legend
import com.rifttally.core.model.Player
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.Side
import com.rifttally.core.model.SideId
import com.rifttally.core.model.UserSettings
import com.rifttally.core.testing.MainDispatcherRule
import com.rifttally.core.testing.TestClock
import com.rifttally.core.testing.repository.FakeGameRepository
import com.rifttally.core.testing.repository.FakeLegendRepository
import com.rifttally.core.testing.repository.FakeUserSettingsRepository
import com.rifttally.feature.tracker.navigation.TrackerRoute
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTime::class)
class TrackerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val games = FakeGameRepository()
    private val legends =
        FakeLegendRepository(listOf(Legend("jinx", "Jinx", "file:///android_asset/legends/jinx.webp")))
    private val settings = FakeUserSettingsRepository(UserSettings(hapticsEnabled = false))
    private var eventIds = 0
    private val factory = GameEventFactory(TestClock(), IdGenerator { "e${++eventIds}" })

    private val config = GameConfig(
        format = GameFormat.TWO_PLAYERS,
        sides = listOf(Side(S1, listOf(P1)), Side(S2, listOf(P2))),
        players = listOf(Player(P1, "Ana", "orange", legendId = "jinx"), Player(P2, "Ben", "sky")),
        targetScore = 3,
    )

    /** [FakeGameRepository] numbers games from 1; each test creates exactly one before the ViewModel. */
    private val gameId = GameId("game1")

    private suspend fun viewModel(): TrackerViewModel {
        check(games.createGame(config) == gameId)
        return TrackerViewModel(
            savedStateHandle = SavedStateHandle(mapOf(TrackerRoute::gameId.name to gameId.value)),
            gameRepository = games,
            legendRepository = legends,
            userSettingsRepository = settings,
            eventFactory = factory,
        )
    }

    /** Runs [block] while the UI state is collected (as the screen would), with an up-to-date VM. */
    private fun withViewModel(block: suspend TestScope.(TrackerViewModel) -> Unit) = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            block(viewModel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun TrackerViewModel.success(): TrackerUiState.Success = assertIs(uiState.value)

    private fun events(): List<GameEvent> = games.allGames.first { it.id == gameId }.events

    @Test
    fun initialState_showsBothPanelsWithLegend() = withViewModel { viewModel ->
        val state = viewModel.success()
        assertEquals(listOf(S1, S2), state.panels.map { it.sideId })
        assertEquals(listOf(0, 0), state.panels.map { it.points })
        assertEquals("Jinx", state.panels[0].players.single().legendName)
        assertEquals("file:///android_asset/legends/jinx.webp", state.panels[0].players.single().legendImageUri)
        assertNull(state.panels[1].players.single().legendName)
        assertEquals(3, state.targetScore)
        assertTrue(!state.canUndo)
        assertNull(state.winnerBanner)
    }

    @Test
    fun pointsChange_appendsEvent_andUpdatesPanel() = withViewModel { viewModel ->
        viewModel.onPointsChange(S1, 1)
        viewModel.onPointsChange(S1, 1)

        assertEquals(2, viewModel.success().panels[0].points)
        assertTrue(viewModel.success().canUndo)
        assertEquals(2, events().size)
    }

    @Test
    fun minusAtZero_appendsNothing() = withViewModel { viewModel ->
        viewModel.onPointsChange(S2, -1)

        assertEquals(emptyList(), events())
        assertEquals(0, viewModel.success().panels[1].points)
    }

    @Test
    fun rapidMinusTaps_atOne_appendOnlyOneEvent() = runTest {
        val viewModel = viewModel()
        games.appendEvent(gameId, factory.pointsAdjusted(S1, 1))
        viewModel.uiState.test {
            expectMostRecentItem()
            // Without a collector-driven re-emission in between, the ViewModel must still know it hit 0.
            repeat(3) { viewModel.onPointsChange(S1, -1) }
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(1, -1), events().map { (it as GameEvent.PointsAdjusted).delta })
    }

    @Test
    fun undo_onEmptyLog_doesNothing() = withViewModel { viewModel ->
        viewModel.onUndo()

        assertEquals(emptyList(), events())
        assertTrue(!viewModel.success().canUndo)
    }

    @Test
    fun keepPlaying_bannerReturnsAfterUndoBelowAndReachingAgain() = withViewModel { viewModel ->
        repeat(3) { viewModel.onPointsChange(S2, 1) }
        viewModel.onKeepPlaying()
        assertNull(viewModel.success().winnerBanner)

        viewModel.onUndo()
        viewModel.onPointsChange(S2, 1)

        assertNotNull(viewModel.success().winnerBanner)
    }

    @Test
    fun doubleRestart_createsOneGame() = withViewModel { viewModel ->
        viewModel.onRestart()
        viewModel.onRestart()
        viewModel.onEndGame()

        assertEquals(2, games.allGames.size)
        assertEquals(1, events().count { it is GameEvent.GameEnded })
    }

    @Test
    fun doubleEnd_appendsOneGameEnded() = withViewModel { viewModel ->
        viewModel.onEndGame()
        viewModel.onEndGame()

        assertEquals(1, events().count { it is GameEvent.GameEnded })
    }

    @Test
    fun history_skipsEventsThatChangedNothing() = runTest {
        val viewModel = viewModel()
        // Written by some other path: a −1 at 0 is a no-op and gets no history line.
        games.appendEvent(gameId, factory.pointsAdjusted(S1, -1))
        games.appendEvent(gameId, factory.pointsAdjusted(S1, 1))
        viewModel.uiState.test {
            val history = assertIs<TrackerUiState.Success>(expectMostRecentItem()).history
            assertEquals<List<HistoryItemUiState>>(listOf(HistoryItemUiState.Points("e2", "Ana", 1, 1)), history)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun undo_removesLastChange() = withViewModel { viewModel ->
        viewModel.onPointsChange(S1, 1)
        viewModel.onPointsChange(S2, 1)

        viewModel.onUndo()

        assertEquals(listOf(1, 0), viewModel.success().panels.map { it.points })
    }

    @Test
    fun reachingTarget_highlightsPanel_andShowsBanner() = withViewModel { viewModel ->
        repeat(3) { viewModel.onPointsChange(S2, 1) }

        val state = viewModel.success()
        assertTrue(state.panels[1].isAtTarget)
        assertTrue(!state.panels[0].isAtTarget)
        assertEquals(listOf("Ben"), assertNotNull(state.winnerBanner).names)
    }

    @Test
    fun keepPlaying_hidesBanner_untilSideDropsAndReachesAgain() = withViewModel { viewModel ->
        repeat(3) { viewModel.onPointsChange(S2, 1) }
        viewModel.onKeepPlaying()
        assertNull(viewModel.success().winnerBanner)

        // Still above target: banner stays hidden; scores can still change.
        viewModel.onPointsChange(S2, 1)
        assertNull(viewModel.success().winnerBanner)

        viewModel.onPointsChange(S2, -2)
        viewModel.onPointsChange(S2, 1)
        assertNotNull(viewModel.success().winnerBanner)
    }

    @Test
    fun keepPlaying_bannerReturnsWhenAnotherSideReachesTarget() = withViewModel { viewModel ->
        repeat(3) { viewModel.onPointsChange(S2, 1) }
        viewModel.onKeepPlaying()
        repeat(3) { viewModel.onPointsChange(S1, 1) }

        assertEquals(listOf("Ana", "Ben"), assertNotNull(viewModel.success().winnerBanner).names)
    }

    @Test
    fun targetScoreChange_appendsEvent_andReevaluatesTarget() = withViewModel { viewModel ->
        repeat(3) { viewModel.onPointsChange(S1, 1) }
        assertTrue(viewModel.success().panels[0].isAtTarget)

        viewModel.onTargetScoreChange(5)

        val state = viewModel.success()
        assertEquals(5, state.targetScore)
        assertTrue(!state.panels[0].isAtTarget)
        assertNull(state.winnerBanner)
    }

    @Test
    fun targetScoreChange_ignoresInvalidOrUnchanged() = withViewModel { viewModel ->
        viewModel.onTargetScoreChange(0)
        viewModel.onTargetScoreChange(3)

        assertEquals(emptyList(), events())
    }

    @Test
    fun endGame_withSingleSideAtTarget_namesWinner_andExits() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            repeat(3) { viewModel.onPointsChange(S1, 1) }
            viewModel.navigationEvents.test {
                viewModel.onEndGame()
                assertEquals(TrackerNavigationEvent.Exit, awaitItem())
            }
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(GameEvent.GameEnded(id = "e4", timestamp = 0, winner = S1), events().last())
        assertNull(games.activeGame.first())
    }

    @Test
    fun endGame_withoutSideAtTarget_hasNoWinner() = withViewModel { viewModel ->
        viewModel.onPointsChange(S1, 1)
        viewModel.onEndGame()

        assertNull(assertIs<GameEvent.GameEnded>(events().last()).winner)
    }

    @Test
    fun restart_endsGame_andOpensNewGameWithSameConfigAndCurrentTarget() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            viewModel.onPointsChange(S1, 1)
            viewModel.onTargetScoreChange(10)
            viewModel.navigationEvents.test {
                viewModel.onRestart()
                val event = assertIs<TrackerNavigationEvent.OpenGame>(awaitItem())
                val newGame = games.allGames.first { it.id == event.gameId }
                assertEquals(config.copy(targetScore = 10), newGame.config)
                assertEquals(emptyList(), newGame.events)
            }
            cancelAndIgnoreRemainingEvents()
        }
        assertIs<GameEvent.GameEnded>(events().last())
    }

    @Test
    fun history_isNewestFirst_withTotals() = withViewModel { viewModel ->
        viewModel.onPointsChange(S1, 1)
        viewModel.onPointsChange(S1, 1)
        viewModel.onPointsChange(S2, 1)
        viewModel.onTargetScoreChange(9)

        assertEquals(
            listOf(
                HistoryItemUiState.TargetChanged("e4", 9),
                HistoryItemUiState.Points("e3", "Ben", 1, 1),
                HistoryItemUiState.Points("e2", "Ana", 1, 2),
                HistoryItemUiState.Points("e1", "Ana", 1, 1),
            ),
            viewModel.success().history,
        )
    }

    @Test
    fun settings_areExposed() = withViewModel { viewModel ->
        settings.setImmersiveMode(false)
        settings.setKeepScreenOn(true)

        val state = viewModel.success()
        assertTrue(!state.immersiveMode)
        assertTrue(state.keepScreenOn)
        assertTrue(!state.hapticsEnabled)
    }

    @Test
    fun unknownGame_isNotFound() = runTest {
        val viewModel = TrackerViewModel(
            savedStateHandle = SavedStateHandle(mapOf(TrackerRoute::gameId.name to "missing")),
            gameRepository = games,
            legendRepository = legends,
            userSettingsRepository = settings,
            eventFactory = factory,
        )
        viewModel.uiState.test {
            assertEquals(TrackerUiState.NotFound, expectMostRecentItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        val P1 = PlayerId("p1")
        val P2 = PlayerId("p2")
        val S1 = SideId("s1")
        val S2 = SideId("s2")
    }
}
