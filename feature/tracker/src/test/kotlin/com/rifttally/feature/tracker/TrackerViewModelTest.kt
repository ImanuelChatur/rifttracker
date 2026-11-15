package com.rifttally.feature.tracker

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rifttally.core.data.game.FirstPlayerPicker
import com.rifttally.core.data.timer.RoundTimer
import com.rifttally.core.game.GameEvent
import com.rifttally.core.game.GameEventFactory
import com.rifttally.core.game.GameLogLine
import com.rifttally.core.game.IdGenerator
import com.rifttally.core.model.Counter
import com.rifttally.core.model.CounterId
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
import com.rifttally.core.testing.repository.FakeRoundTimerRepository
import com.rifttally.core.testing.repository.FakeUserSettingsRepository
import com.rifttally.feature.tracker.navigation.TrackerRoute
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTime::class)
class TrackerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val games = FakeGameRepository()
    private val legends =
        FakeLegendRepository(
            listOf(
                Legend(
                    "jinx",
                    "Jinx",
                    "file:///android_asset/legends/jinx.webp",
                    "file:///android_asset/legends/jinx_wide.webp",
                ),
            ),
        )
    private val settings = FakeUserSettingsRepository(UserSettings(hapticsEnabled = false))
    private var eventIds = 0
    private val factory = GameEventFactory(TestClock(), IdGenerator { "e${++eventIds}" })

    /** The index the "random" pick returns. */
    private var pickIndex = 1
    private val picker = FirstPlayerPicker(settings, { pickIndex }, factory)

    /** Wall clock of the round timer (the event factory has its own). */
    private val timerClock = TestClock()
    private val timers = FakeRoundTimerRepository(timerClock)

    private val config = GameConfig(
        format = GameFormat.TWO_PLAYERS,
        sides = listOf(Side(S1, listOf(P1)), Side(S2, listOf(P2))),
        players = listOf(Player(P1, "Ana", "orange", legendId = "jinx"), Player(P2, "Ben", "sky")),
        targetScore = 3,
    )

    /** [FakeGameRepository] numbers games from 1; each test creates exactly one before the ViewModel. */
    private val gameId = GameId("game1")

    private suspend fun viewModel(): TrackerViewModel {
        check(games.createGame(config, emptyList()) == gameId)
        return TrackerViewModel(
            savedStateHandle = SavedStateHandle(mapOf(TrackerRoute::gameId.name to gameId.value)),
            gameRepository = games,
            legendRepository = legends,
            userSettingsRepository = settings,
            eventFactory = factory,
            firstPlayerPicker = picker,
            timerRepository = timers,
            clock = timerClock,
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
        assertEquals(
            "file:///android_asset/legends/jinx_wide.webp",
            state.panels[0].players.single().legendWideImageUri,
        )
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
    fun staleEmission_doesNotUnblockMinusAtZero() = withViewModel { viewModel ->
        games.emitStale(gameId, eventCount = 0) // hold Room's emissions
        viewModel.onPointsChange(S1, 1) // A
        viewModel.onPointsChange(S1, -1) // B: back to 0
        assertEquals(2, events().size)
        games.emitStale(gameId, eventCount = 1) // a late emission with only A (1 point)
        viewModel.onPointsChange(S1, -1) // C: must still be blocked
        games.flush()

        assertEquals(listOf("e1", "e2"), events().map { it.id })
        assertEquals(listOf(1, -1), events().map { (it as GameEvent.PointsAdjusted).delta })
        assertEquals(0, viewModel.success().panels[0].points)
    }

    @Test
    fun staleEmission_showsOptimisticScore_thenFollowsDatabaseOnceInSync() = withViewModel { viewModel ->
        games.emitStale(gameId, eventCount = 0)
        viewModel.onPointsChange(S1, 1)
        viewModel.onPointsChange(S1, 1)
        games.emitStale(gameId, eventCount = 1)

        assertEquals(2, viewModel.success().panels[0].points)

        games.flush()
        assertEquals(2, viewModel.success().panels[0].points)
        // In sync again: a change that didn't come from this screen is followed.
        games.appendEvent(gameId, factory.pointsAdjusted(S2, 1))
        assertEquals(1, viewModel.success().panels[1].points)
    }

    @Test
    fun plusThenUndo_inFlight_oldMatchingEmission_doesNotUnblockMinus() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            expectMostRecentItem()
            val gate = CompletableDeferred<Unit>()
            games.beforeWrite = { gate.await() } // both writes stay in flight
            viewModel.onPointsChange(S1, 1) // A (e1)
            viewModel.onUndo()
            games.emitStale(gameId, events = emptyList()) // from before A: same ids as the optimistic log
            games.emitStale(gameId, events = listOf(GameEvent.PointsAdjusted("e1", 0, S1, 1))) // stale [A]
            viewModel.onPointsChange(S1, -1) // must be blocked: the score is 0
            games.beforeWrite = null
            gate.complete(Unit)
            advanceUntilIdle()
            games.flush()
            advanceUntilIdle()

            assertEquals(emptyList(), events())
            assertEquals(0, viewModel.success().panels[0].points)
            // In sync again: a change that didn't come from this screen is followed.
            games.appendEvent(gameId, factory.pointsAdjusted(S2, 1))
            assertEquals(1, viewModel.success().panels[1].points)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun staleEmission_afterUndo_keepsTheUndo() = withViewModel { viewModel ->
        viewModel.onPointsChange(S1, 1)
        viewModel.onPointsChange(S1, 1)
        games.emitStale(gameId, eventCount = 2)
        viewModel.onUndo()

        assertEquals(1, viewModel.success().panels[0].points)
        games.flush()
        assertEquals(1, viewModel.success().panels[0].points)
        assertEquals(1, events().size)
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
            assertEquals<List<GameLogLine>>(listOf(GameLogLine.Points("e2", "Ana", 1, 1)), history)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun TrackerViewModel.counters(player: Int) = success().panels[player].players.single().counters

    @Test
    fun counterAdd_showsUnderThatPlayer_withTrimmedLabel() = withViewModel { viewModel ->
        viewModel.onCounterAdd(P1, "  XP  ")
        viewModel.onCounterAdd(P1, "   ")

        assertEquals(listOf("XP" to 0), viewModel.counters(0).map { it.label to it.value })
        assertEquals(emptyList(), viewModel.counters(1))
    }

    @Test
    fun counterAdd_capsLabelLength() = withViewModel { viewModel ->
        viewModel.onCounterAdd(P2, "A very long counter label")

        assertEquals(MAX_COUNTER_LABEL_LENGTH, viewModel.counters(1).single().label.length)
    }

    @Test
    fun counterChange_adjusts_andFloorsAtZeroWithoutNoOpEvents() = withViewModel { viewModel ->
        viewModel.onCounterAdd(P1, "XP")
        val xp = viewModel.counters(0).single().id

        viewModel.onCounterChange(P1, xp, 1)
        viewModel.onCounterChange(P1, xp, 1)
        repeat(4) { viewModel.onCounterChange(P1, xp, -1) }

        assertEquals(0, viewModel.counters(0).single().value)
        // add + 2 up + 2 down; the extra −1s at 0 were never appended.
        assertEquals(5, events().size)
    }

    @Test
    fun counterRemove_removesIt_andUndoRestoresIt() = withViewModel { viewModel ->
        viewModel.onCounterAdd(P1, "XP")
        val xp = viewModel.counters(0).single().id
        viewModel.onCounterChange(P1, xp, 3)

        viewModel.onCounterRemove(P1, xp)
        assertEquals(emptyList(), viewModel.counters(0))

        viewModel.onUndo()
        assertEquals(listOf("XP" to 3), viewModel.counters(0).map { it.label to it.value })
    }

    @Test
    fun counterAdd_isCappedPerPlayer() = withViewModel { viewModel ->
        repeat(MAX_COUNTERS_PER_PLAYER + 2) { viewModel.onCounterAdd(P1, "C$it") }

        assertEquals(MAX_COUNTERS_PER_PLAYER, viewModel.counters(0).size)
    }

    @Test
    fun counterActions_afterGameEnded_appendNothing() = withViewModel { viewModel ->
        viewModel.onCounterAdd(P1, "XP")
        val xp = viewModel.counters(0).single().id
        viewModel.onEndGame()
        val before = events()

        viewModel.onCounterAdd(P1, "Energy")
        viewModel.onCounterChange(P1, xp, 1)
        viewModel.onCounterRemove(P1, xp)

        assertEquals(before, events())
    }

    @Test
    fun counterEvents_forUnknownCounter_areIgnored() = withViewModel { viewModel ->
        viewModel.onCounterChange(P1, CounterId("nope"), 1)
        viewModel.onCounterRemove(P1, CounterId("nope"))

        assertEquals(emptyList(), events())
    }

    @Test
    fun initialCounters_fromConfig_areShown() = runTest {
        val withXp = config.copy(
            initialCounters = mapOf(P2 to listOf(Counter(XP_ID, "XP", 0))),
        )
        check(games.createGame(withXp, emptyList()) == gameId)
        val viewModel = TrackerViewModel(
            savedStateHandle = SavedStateHandle(mapOf(TrackerRoute::gameId.name to gameId.value)),
            gameRepository = games,
            legendRepository = legends,
            userSettingsRepository = settings,
            eventFactory = factory,
            firstPlayerPicker = picker,
            timerRepository = timers,
            clock = timerClock,
        )
        viewModel.uiState.test {
            val panel = assertIs<TrackerUiState.Success>(expectMostRecentItem()).panels[1]
            assertEquals(listOf(CounterUiState(XP_ID, "XP", 0)), panel.players.single().counters)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun counterHistory_isListed() = withViewModel { viewModel ->
        viewModel.onCounterAdd(P1, "XP")
        val xp = viewModel.counters(0).single().id
        viewModel.onCounterChange(P1, xp, 2)

        assertEquals(
            listOf<GameLogLine>(
                GameLogLine.CounterChanged("e3", "Ana", "XP", 2, 2),
                // The counter id is generated first (e1), then the event id (e2).
                GameLogLine.CounterAdded("e2", "Ana", "XP"),
            ),
            viewModel.success().history,
        )
    }

    private suspend fun viewModelFor(gameConfig: GameConfig): TrackerViewModel {
        check(games.createGame(gameConfig, emptyList()) == gameId)
        return TrackerViewModel(
            savedStateHandle = SavedStateHandle(mapOf(TrackerRoute::gameId.name to gameId.value)),
            gameRepository = games,
            legendRepository = legends,
            userSettingsRepository = settings,
            eventFactory = factory,
            firstPlayerPicker = picker,
            timerRepository = timers,
            clock = timerClock,
        )
    }

    @Test
    fun teams_twoPanels_withBothTeammates_andSharedPoints() = runTest {
        val p3 = PlayerId("p3")
        val p4 = PlayerId("p4")
        val teamA = SideId("team_a")
        val teams = GameConfig(
            format = GameFormat.TEAMS_2V2,
            sides = listOf(Side(teamA, listOf(P1, p3)), Side(SideId("team_b"), listOf(P2, p4))),
            players = listOf(
                Player(P1, "Ana", "orange", "jinx"),
                Player(P2, "Ben", "sky"),
                Player(p3, "Cleo", "green"),
                Player(p4, "Dan", "pink"),
            ),
        )
        val viewModel = viewModelFor(teams)
        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onPointsChange(teamA, 1)
            viewModel.onCounterAdd(p3, "XP")
            val state = assertIs<TrackerUiState.Success>(expectMostRecentItem())
            assertEquals(2, state.panels.size)
            assertEquals(listOf("Ana", "Cleo"), state.panels[0].players.map { it.name })
            assertEquals("Ana & Cleo", state.panels[0].displayName)
            assertEquals(1, state.panels[0].points)
            assertEquals(11, state.targetScore)
            // Counters stay per player inside the team.
            assertEquals(emptyList(), state.panels[0].players[0].counters)
            assertEquals(listOf("XP"), state.panels[0].players[1].counters.map { it.label })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun fourPlayers_fourPanelsInSeatOrder() = runTest {
        val ids = (1..4).map { PlayerId("p$it") }
        val four = GameConfig(
            format = GameFormat.FOUR_PLAYERS,
            sides = ids.mapIndexed { i, id -> Side(SideId("s${i + 1}"), listOf(id)) },
            players = ids.mapIndexed { i, id -> Player(id, "P${i + 1}", "orange") },
        )
        val viewModel = viewModelFor(four)
        viewModel.uiState.test {
            val state = assertIs<TrackerUiState.Success>(expectMostRecentItem())
            assertEquals(listOf("P1", "P2", "P3", "P4"), state.panels.map { it.displayName })
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
    fun endGame_withSingleSideAtTarget_namesWinner_andShowsGameOver() = withViewModel { viewModel ->
        repeat(3) { viewModel.onPointsChange(S1, 1) }
        viewModel.onPointsChange(S2, 1)

        viewModel.onEndGame()

        assertEquals(GameEvent.GameEnded(id = "e5", timestamp = 0, winner = S1), events().last())
        assertNull(games.activeGame.first())
        assertEquals(
            GameOverUiState(
                winnerName = "Ana",
                sides = persistentListOf(
                    GameOverSideUiState("Ana", "orange", 3, isWinner = true),
                    GameOverSideUiState("Ben", "sky", 1, isWinner = false),
                ),
            ),
            viewModel.success().gameOver,
        )
        assertNull(viewModel.success().winnerBanner)
    }

    @Test
    fun gameOver_withoutWinner_saysSo() = withViewModel { viewModel ->
        viewModel.onEndGame()

        val gameOver = assertNotNull(viewModel.success().gameOver)
        assertNull(gameOver.winnerName)
        assertTrue(gameOver.sides.none { it.isWinner })
    }

    @Test
    fun undoOnGameOver_reopensTheGame() = withViewModel { viewModel ->
        viewModel.onPointsChange(S1, 1)
        viewModel.onEndGame()

        viewModel.onUndo()

        assertNull(viewModel.success().gameOver)
        assertEquals(gameId, games.activeGame.first()?.id)
        // Playable again.
        viewModel.onPointsChange(S1, 1)
        assertEquals(2, viewModel.success().panels[0].points)
    }

    @Test
    fun pointsWhileGameOver_areIgnored() = withViewModel { viewModel ->
        viewModel.onEndGame()
        viewModel.onPointsChange(S1, 1)
        viewModel.onCounterAdd(P1, "XP")

        assertIs<GameEvent.GameEnded>(events().single())
    }

    @Test
    fun rematch_opensANewGameWithTheSameLineupAndFinalTarget() = runTest {
        val withXp = config.copy(initialCounters = mapOf(P1 to listOf(Counter(XP_ID, "XP", 0))))
        val viewModel = viewModelFor(withXp)
        viewModel.uiState.test {
            viewModel.onTargetScoreChange(5)
            viewModel.onCounterAdd(P2, "Gold") // mid-game counters aren't carried over
            viewModel.onEndGame()
            viewModel.navigationEvents.test {
                viewModel.onRematch()
                viewModel.onRematch() // double tap: still one new game
                val event = assertIs<TrackerNavigationEvent.OpenGame>(awaitItem())
                val newGame = games.allGames.first { it.id == event.gameId }
                assertEquals(withXp.copy(targetScore = 5), newGame.config)
                // With the default setting, the rematch opens with its own first-player pick.
                assertIs<GameEvent.FirstPlayerChosen>(newGame.events.single())
            }
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(2, games.allGames.size)
        assertEquals(1, games.allGames.count { it.endedAt == null })
    }

    @Test
    fun undo_whileRematchIsCreatingTheGame_isIgnored() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            viewModel.onEndGame()
            advanceUntilIdle()
            val gate = CompletableDeferred<Unit>()
            games.beforeCreate = { gate.await() }
            viewModel.onRematch()
            viewModel.onUndo() // must not reopen the old game while the new one is created
            games.beforeCreate = null
            gate.complete(Unit)
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
        assertIs<GameEvent.GameEnded>(events().single())
        assertEquals(1, games.allGames.count { it.endedAt == null })
    }

    @Test
    fun rematch_beforeTheEnd_doesNothing() = withViewModel { viewModel ->
        viewModel.onRematch()

        assertEquals(1, games.allGames.size)
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
                // "Pick first player at game start" is on by default: the new game opens with a pick.
                assertEquals(listOf(P2), newGame.events.map { (it as GameEvent.FirstPlayerChosen).playerId })
            }
            cancelAndIgnoreRemainingEvents()
        }
        assertIs<GameEvent.GameEnded>(events().last())
        // One active game: the new one.
        assertEquals(1, games.allGames.count { it.endedAt == null })
    }

    @Test
    fun restart_withPickSettingOff_opensAnEmptyGame() = runTest {
        settings.setPickFirstPlayer(false)
        val viewModel = viewModel()
        viewModel.uiState.test {
            viewModel.navigationEvents.test {
                viewModel.onRestart()
                val event = assertIs<TrackerNavigationEvent.OpenGame>(awaitItem())
                assertEquals(emptyList(), games.allGames.first { it.id == event.gameId }.events)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // region Round timer

    /** Runs [block] while the timer state is collected (as the visible screen does). */
    private fun withTimer(minutes: Int?, block: suspend TestScope.(TrackerViewModel) -> Unit) = runTest {
        val viewModel = viewModel()
        if (minutes != null) timers.createTimer(gameId, minutes)
        backgroundScope.launch { viewModel.timerState.collect {} }
        runCurrent()
        block(viewModel)
    }

    private fun TrackerViewModel.timer(): RoundTimerUiState = assertNotNull(timerState.value)

    @Test
    fun noTimer_showsNoTimer() = withTimer(minutes = null) { viewModel ->
        assertNull(viewModel.timerState.value)
    }

    @Test
    fun freshTimer_isFullLengthAndPaused_andDoesNotMove() = withTimer(minutes = 50) { viewModel ->
        assertEquals(
            RoundTimerUiState(isRunning = false, seconds = 3_000, phase = RoundTimerPhase.Normal),
            viewModel.timer(),
        )

        timerClock.advanceBy(10.minutes)
        advanceTimeBy(10.minutes)
        runCurrent()

        assertEquals(3_000, viewModel.timer().seconds)
    }

    @Test
    fun running_ticksOncePerSecond_fromTheInjectedClock() = withTimer(minutes = 50) { viewModel ->
        viewModel.onTimerToggle()
        runCurrent()
        assertEquals(
            RoundTimerUiState(isRunning = true, seconds = 3_000, phase = RoundTimerPhase.Normal),
            viewModel.timer(),
        )

        timerClock.advanceBy(1.seconds)
        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(2_999, viewModel.timer().seconds)

        // 12:30 left.
        timerClock.advanceBy(37.minutes + 29.seconds)
        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(12L to 30L, viewModel.timer().let { it.minutesPart to it.secondsPart })
    }

    @Test
    fun lastFiveMinutes_isWarning_thenOvertimeCountsUp() = withTimer(minutes = 10) { viewModel ->
        viewModel.onTimerToggle()
        runCurrent()

        timerClock.advanceBy(4.minutes + 59.seconds)
        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(RoundTimerPhase.Normal, viewModel.timer().phase) // 5:01 left

        timerClock.advanceBy(1.seconds)
        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(
            RoundTimerUiState(isRunning = true, seconds = 300, phase = RoundTimerPhase.Warning),
            viewModel.timer(),
        )

        timerClock.advanceBy(5.minutes)
        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(
            RoundTimerUiState(isRunning = true, seconds = 0, phase = RoundTimerPhase.Overtime),
            viewModel.timer(),
        )

        timerClock.advanceBy(2.minutes + 13.seconds)
        advanceTimeBy(1.seconds)
        runCurrent()
        assertEquals(
            RoundTimerUiState(isRunning = true, seconds = 133, phase = RoundTimerPhase.Overtime),
            viewModel.timer(),
        )
    }

    @Test
    fun pause_stopsTheCountdown_andResetGoesBackToFullPaused() = withTimer(minutes = 50) { viewModel ->
        viewModel.onTimerToggle()
        timerClock.advanceBy(90.seconds)
        viewModel.onTimerToggle()
        runCurrent()
        assertEquals(
            RoundTimerUiState(isRunning = false, seconds = 2_910, phase = RoundTimerPhase.Normal),
            viewModel.timer(),
        )

        timerClock.advanceBy(10.minutes)
        advanceTimeBy(2.seconds)
        runCurrent()
        assertEquals(2_910, viewModel.timer().seconds)

        viewModel.onTimerToggle()
        timerClock.advanceBy(5.seconds)
        viewModel.onTimerReset()
        runCurrent()
        assertEquals(
            RoundTimerUiState(isRunning = false, seconds = 3_000, phase = RoundTimerPhase.Normal),
            viewModel.timer(),
        )
    }

    @Test
    fun undo_neverTouchesTheTimer() = withTimer(minutes = 50) { viewModel ->
        viewModel.uiState.test {
            viewModel.onPointsChange(S1, 1)
            viewModel.onTimerToggle()
            runCurrent()
            val before = timers.allTimers.getValue(gameId)
            viewModel.onUndo()
            viewModel.onUndo()
            runCurrent()
            assertEquals(before, timers.allTimers.getValue(gameId))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun timer_survivesViewModelRecreation() = runTest {
        val first = viewModel()
        timers.createTimer(gameId, 50)
        first.onTimerToggle()
        timerClock.advanceBy(10.minutes)

        // A new ViewModel (e.g. after process death) reads the stored timer: still running, 40:00 left.
        val second = TrackerViewModel(
            savedStateHandle = SavedStateHandle(mapOf(TrackerRoute::gameId.name to gameId.value)),
            gameRepository = games,
            legendRepository = legends,
            userSettingsRepository = settings,
            eventFactory = factory,
            firstPlayerPicker = picker,
            timerRepository = timers,
            clock = timerClock,
        )
        backgroundScope.launch { second.timerState.collect {} }
        runCurrent()
        assertEquals(
            RoundTimerUiState(isRunning = true, seconds = 2_400, phase = RoundTimerPhase.Normal),
            second.timer(),
        )
    }

    @Test
    fun rematch_carriesTheTimerOverUnchanged() = withTimer(minutes = 50) { viewModel ->
        viewModel.uiState.test {
            viewModel.onTimerToggle()
            timerClock.advanceBy(7.minutes)
            viewModel.onEndGame()
            viewModel.navigationEvents.test {
                viewModel.onRematch()
                val event = assertIs<TrackerNavigationEvent.OpenGame>(awaitItem())
                // Same length, same start, still running: the round goes on.
                assertEquals(timers.allTimers.getValue(gameId), timers.allTimers.getValue(event.gameId))
                assertTrue(timers.allTimers.getValue(event.gameId).isRunning)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun restart_carriesTheTimerOverUnchanged() = withTimer(minutes = 30) { viewModel ->
        viewModel.uiState.test {
            viewModel.onTimerToggle()
            timerClock.advanceBy(3.minutes)
            viewModel.onTimerToggle() // paused at 27:00
            viewModel.navigationEvents.test {
                viewModel.onRestart()
                val event = assertIs<TrackerNavigationEvent.OpenGame>(awaitItem())
                assertEquals(
                    RoundTimer(durationMs = 30 * 60_000L, elapsedBeforeMs = 3 * 60_000L),
                    timers.allTimers[event.gameId],
                )
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun rematch_withoutTimer_createsNoTimer() = withTimer(minutes = null) { viewModel ->
        viewModel.uiState.test {
            viewModel.onEndGame()
            viewModel.navigationEvents.test {
                viewModel.onRematch()
                awaitItem()
            }
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(emptyMap(), timers.allTimers)
    }

    // endregion

    // region First player

    /** A tracker on a game that opened with a pick of [P2] (as Setup creates it), sharing [handle]. */
    private fun trackerOnPickedGame(handle: SavedStateHandle) = TrackerViewModel(
        savedStateHandle = handle,
        gameRepository = games,
        legendRepository = legends,
        userSettingsRepository = settings,
        eventFactory = factory,
        firstPlayerPicker = picker,
        timerRepository = timers,
        clock = timerClock,
    )

    private suspend fun createPickedGame(): GameEvent.FirstPlayerChosen {
        val pick = factory.firstPlayerChosen(P2)
        check(games.createGame(config, listOf(pick)) == gameId)
        return pick
    }

    private fun handle() = SavedStateHandle(mapOf(TrackerRoute::gameId.name to gameId.value))

    @Test
    fun pickAtCreation_isRevealedOnItsSeat_withMarker() = runTest {
        val pick = createPickedGame()
        val viewModel = trackerOnPickedGame(handle())
        viewModel.uiState.test {
            val state = assertIs<TrackerUiState.Success>(expectMostRecentItem())
            assertEquals(FirstPlayerRevealUiState(pick.id, panelIndex = 1, name = "Ben"), state.firstPlayerReveal)
            assertEquals(listOf(null, "Ben"), state.panels.map { it.firstPlayerName })
            assertEquals(listOf<GameLogLine>(GameLogLine.FirstPlayer(pick.id, "Ben")), state.history)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun reveal_isShownOnce_evenAfterRecreation() = runTest {
        val pick = createPickedGame()
        val handle = handle()
        val viewModel = trackerOnPickedGame(handle)
        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onFirstPlayerRevealStart(pick.id)
            // Started (e.g. then rotated mid-cycle): the result shows without cycling again.
            assertEquals(false, assertIs<TrackerUiState.Success>(expectMostRecentItem()).firstPlayerReveal?.animate)
            viewModel.onFirstPlayerRevealDismiss(pick.id)
            assertNull(assertIs<TrackerUiState.Success>(expectMostRecentItem()).firstPlayerReveal)
            cancelAndIgnoreRemainingEvents()
        }
        // Process death / rotation: a new ViewModel on the same saved state doesn't replay it.
        val recreated = trackerOnPickedGame(handle)
        recreated.uiState.test {
            assertNull(assertIs<TrackerUiState.Success>(expectMostRecentItem()).firstPlayerReveal)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun reopeningAGameUnderWay_doesNotReplayTheReveal() = runTest {
        createPickedGame()
        games.appendEvent(gameId, factory.pointsAdjusted(S1, 1))

        val viewModel = trackerOnPickedGame(handle())
        viewModel.uiState.test {
            val state = assertIs<TrackerUiState.Success>(expectMostRecentItem())
            assertNull(state.firstPlayerReveal)
            // The first points change also hides the "1st" marker.
            assertEquals(listOf(null, null), state.panels.map { it.firstPlayerName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun marker_staysThroughCounterChanges_untilPointsChange() = runTest {
        createPickedGame()
        val viewModel = trackerOnPickedGame(handle())
        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onCounterAdd(P1, "XP")
            assertEquals("Ben", viewModel.success().panels[1].firstPlayerName)
            viewModel.onPointsChange(S2, 1)
            assertNull(viewModel.success().panels[1].firstPlayerName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun menuPick_rerolls_andPlaysTheRevealAgain() = withViewModel { viewModel ->
        viewModel.onPointsChange(S1, 1)
        pickIndex = 0

        viewModel.onPickFirstPlayer()

        val pick = assertIs<GameEvent.FirstPlayerChosen>(events().last())
        assertEquals(P1, pick.playerId)
        val state = viewModel.success()
        assertEquals(FirstPlayerRevealUiState(pick.id, panelIndex = 0, name = "Ana"), state.firstPlayerReveal)
        assertEquals(listOf("Ana", null), state.panels.map { it.firstPlayerName })
        assertEquals(GameLogLine.FirstPlayer(pick.id, "Ana"), state.history.first())
    }

    @Test
    fun menuPick_inTeams_marksThePlayersTeamPanel() = runTest {
        val teams = GameConfig(
            format = GameFormat.TEAMS_2V2,
            sides = listOf(Side(S1, listOf(P1, P3)), Side(S2, listOf(P2, P4))),
            players = listOf(
                Player(P1, "Ana", "orange"),
                Player(P2, "Ben", "sky"),
                Player(P3, "Cleo", "green"),
                Player(P4, "Dan", "pink"),
            ),
        )
        val viewModel = viewModelFor(teams)
        viewModel.uiState.test {
            expectMostRecentItem()
            pickIndex = 2 // Cleo, on the first team
            viewModel.onPickFirstPlayer()
            val state = viewModel.success()
            assertEquals(0, state.firstPlayerReveal?.panelIndex)
            assertEquals("Cleo", state.firstPlayerReveal?.name)
            assertEquals(listOf("Cleo", null), state.panels.map { it.firstPlayerName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun undoingADismissedReroll_doesNotReplayTheEarlierPick() = runTest {
        val first = createPickedGame()
        val viewModel = trackerOnPickedGame(handle())
        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onFirstPlayerRevealDismiss(first.id)
            viewModel.onPickFirstPlayer()
            val reroll = assertNotNull(viewModel.success().firstPlayerReveal)
            viewModel.onFirstPlayerRevealDismiss(reroll.pickId)
            viewModel.onUndo()
            assertEquals(listOf(first.id), events().map { it.id })
            assertNull(viewModel.success().firstPlayerReveal)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun undo_removesThePick_andItsReveal() = withViewModel { viewModel ->
        viewModel.onPickFirstPlayer()
        viewModel.onUndo()

        assertEquals(emptyList(), events())
        assertNull(viewModel.success().firstPlayerReveal)
        assertEquals(listOf(null, null), viewModel.success().panels.map { it.firstPlayerName })
    }

    @Test
    fun menuPick_afterTheGameEnded_doesNothing() = withViewModel { viewModel ->
        viewModel.onEndGame()
        viewModel.onPickFirstPlayer()

        assertIs<GameEvent.GameEnded>(events().single())
    }

    // endregion

    @Test
    fun history_isNewestFirst_withTotals() = withViewModel { viewModel ->
        viewModel.onPointsChange(S1, 1)
        viewModel.onPointsChange(S1, 1)
        viewModel.onPointsChange(S2, 1)
        viewModel.onTargetScoreChange(9)

        assertEquals(
            listOf(
                GameLogLine.TargetChanged("e4", 9),
                GameLogLine.Points("e3", "Ben", 1, 1),
                GameLogLine.Points("e2", "Ana", 1, 2),
                GameLogLine.Points("e1", "Ana", 1, 1),
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
            firstPlayerPicker = picker,
            timerRepository = timers,
            clock = timerClock,
        )
        viewModel.uiState.test {
            assertEquals(TrackerUiState.NotFound, expectMostRecentItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        val P1 = PlayerId("p1")
        val P2 = PlayerId("p2")
        val P3 = PlayerId("p3")
        val P4 = PlayerId("p4")
        val S1 = SideId("s1")
        val S2 = SideId("s2")
        val XP_ID = CounterId("xp")
    }
}
