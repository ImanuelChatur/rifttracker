package com.rifttally.feature.setup

import app.cash.turbine.test
import com.rifttally.core.data.game.FirstPlayerPicker
import com.rifttally.core.data.timer.RoundTimer
import com.rifttally.core.data.timer.RoundTimerChoice
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.game.GameEvent
import com.rifttally.core.game.GameEventFactory
import com.rifttally.core.game.IdGenerator
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
import com.rifttally.core.model.Legend
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.PlayerPreset
import com.rifttally.core.model.SideId
import com.rifttally.core.testing.MainDispatcherRule
import com.rifttally.core.testing.TestClock
import com.rifttally.core.testing.repository.FakeGameRepository
import com.rifttally.core.testing.repository.FakeLegendRepository
import com.rifttally.core.testing.repository.FakePlayerPresetRepository
import com.rifttally.core.testing.repository.FakeRoundTimerRepository
import com.rifttally.core.testing.repository.FakeUserSettingsRepository
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTime::class)
class SetupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val timers = FakeRoundTimerRepository(TestClock())
    private val games = FakeGameRepository(timers)
    private val settings = FakeUserSettingsRepository()
    private val presets = FakePlayerPresetRepository()
    private val legends = FakeLegendRepository(
        listOf(Legend("jinx", "Jinx", null), Legend("viktor", "Viktor", null), Legend("lee_sin", "Lee Sin", null)),
    )

    /** Always picks the second player. */
    private val picker = FirstPlayerPicker(settings, { 1 }, GameEventFactory(TestClock(), IdGenerator { "pick" }))

    // Lazy: created inside each test, after MainDispatcherRule has installed the test Main dispatcher.
    private val viewModel by lazy { SetupViewModel(games, legends, settings, presets, picker, timers) }

    private val defaultNames = listOf("Player 1", "Player 2")

    @Test
    fun initialState_hasTwoPlayersWithDistinctColors_andDefaultTarget() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.players.size)
            assertEquals(2, state.players.map { it.colorId }.distinct().size)
            assertEquals(GameFormat.TWO_PLAYERS.defaultTargetScore, state.targetScore)
            assertNull(state.legendPicker)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun editingPlayers_isReflectedInState() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onNameChange(0, "Ana")
            viewModel.onColorSelect(1, "green")
            val state = expectMostRecentItem()
            assertEquals("Ana", state.players[0].name)
            assertEquals("green", state.players[1].colorId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun legendPicker_filtersByQuery_andSelectsForThatPlayer() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onLegendPickerOpen(1)
            viewModel.onLegendQueryChange("vik")
            val picker = assertNotNull(expectMostRecentItem().legendPicker)
            assertEquals(1, picker.playerIndex)
            assertEquals(listOf("viktor"), picker.legends.map { it.id })

            viewModel.onLegendSelect("viktor")
            val state = expectMostRecentItem()
            assertNull(state.legendPicker)
            assertEquals("viktor", state.players[1].legend?.id)
            assertNull(state.players[0].legend)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun legendPicker_noneClearsLegend() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onLegendPickerOpen(0)
            viewModel.onLegendSelect("jinx")
            viewModel.onLegendPickerOpen(0)
            viewModel.onLegendSelect(null)
            assertNull(expectMostRecentItem().players[0].legend)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun targetScore_cannotGoBelowOne() = runTest {
        viewModel.uiState.test {
            awaitItem()
            repeat(20) { viewModel.onTargetScoreDecrease() }
            val state = expectMostRecentItem()
            assertEquals(1, state.targetScore)
            assertTrue(!state.canDecreaseTarget)
            viewModel.onTargetScoreIncrease()
            assertEquals(2, expectMostRecentItem().targetScore)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun startGame_createsConfig_andOpensTracker() = runTest {
        viewModel.onNameChange(0, "  Ana ")
        viewModel.onLegendPickerOpen(0)
        viewModel.onLegendSelect("jinx")
        viewModel.onTargetScoreIncrease()

        viewModel.navigationEvents.test {
            viewModel.onStartGame(defaultNames, "XP")
            assertEquals(SetupNavigationEvent.OpenTracker(GameId("game1")), awaitItem())
        }

        val config = games.allGames.single().config
        assertEquals(GameFormat.TWO_PLAYERS, config.format)
        assertEquals(listOf("Ana", "Player 2"), config.players.map { it.name })
        assertEquals("jinx", config.players[0].legendId)
        assertEquals(9, config.targetScore)
        assertEquals(listOf(SideId("s1"), SideId("s2")), config.sides.map { it.id })
        config.sides.zip(config.players).forEach { (side, player) -> assertEquals(listOf(player.id), side.playerIds) }
    }

    @Test
    fun startGame_withPickSettingOn_startsTheLogWithAPick() = runTest {
        viewModel.onStartGame(defaultNames, "XP")

        val game = games.allGames.single()
        val pick = game.events.single() as GameEvent.FirstPlayerChosen
        assertEquals(PlayerId("p2"), pick.playerId)
        assertEquals(game.config.players[1].id, pick.playerId)
    }

    @Test
    fun startGame_withPickSettingOff_startsWithAnEmptyLog() = runTest {
        settings.setPickFirstPlayer(false)

        viewModel.onStartGame(defaultNames, "XP")

        assertEquals(emptyList(), games.allGames.single().events)
    }

    @Test
    fun startGame_twice_createsOneGame() = runTest {
        viewModel.onStartGame(defaultNames, "XP")
        viewModel.onStartGame(defaultNames, "XP")

        assertEquals(1, games.allGames.size)
    }

    @Test
    fun xpToggle_isRemembered_andReflectedInState() = runTest {
        viewModel.uiState.test {
            assertTrue(!awaitItem().addXpCounter)
            viewModel.onAddXpCounterToggle(true)
            assertTrue(expectMostRecentItem().addXpCounter)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(settings.current.addXpCounterByDefault)
    }

    @Test
    fun startGame_withXpToggle_givesEveryPlayerAnXpCounter() = runTest {
        settings.setAddXpCounterByDefault(true)

        viewModel.onStartGame(defaultNames, "XP")

        val config = games.allGames.single().config
        assertEquals(config.players.map { it.id }.toSet(), config.initialCounters.keys)
        config.initialCounters.values.forEach { counters ->
            assertEquals(listOf("XP" to 0), counters.map { it.label to it.value })
        }
    }

    @Test
    fun startGame_withoutXpToggle_hasNoCounters() = runTest {
        viewModel.onStartGame(defaultNames, "XP")

        assertTrue(games.allGames.single().config.initialCounters.isEmpty())
    }

    @Test
    fun defaultFormat_comesFromSettings() = runTest {
        settings.setDefaultGameFormat(GameFormat.FOUR_PLAYERS)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(GameFormat.FOUR_PLAYERS, state.format)
            assertEquals(4, state.players.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun formatSelect_resizesPlayers_keepsEnteredOnes_andResetsTarget() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onNameChange(0, "Ana")
            viewModel.onTargetScoreIncrease()

            viewModel.onFormatSelect(GameFormat.THREE_PLAYERS)
            val three = expectMostRecentItem()
            assertEquals(3, three.players.size)
            assertEquals("Ana", three.players[0].name)
            assertEquals(GameFormat.THREE_PLAYERS.defaultTargetScore, three.targetScore)
            assertEquals(3, three.players.map { it.colorId }.distinct().size)
            assertTrue(three.players.all { it.team == null })

            viewModel.onFormatSelect(GameFormat.TEAMS_2V2)
            val teams = expectMostRecentItem()
            assertEquals(11, teams.targetScore)
            assertEquals(listOf(Team.A, Team.A, Team.B, Team.B), teams.players.map { it.team })
            assertTrue(teams.canStart)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun teams_unbalanced_cannotStart() = runTest {
        viewModel.onFormatSelect(GameFormat.TEAMS_2V2)
        viewModel.onTeamSelect(2, Team.A)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.teamsInvalid)
            assertTrue(!state.canStart)
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.onStartGame(listOf("P1", "P2", "P3", "P4"), "XP")
        assertTrue(games.allGames.isEmpty())
    }

    @Test
    fun startGame_teams_buildsTwoSharedSides() = runTest {
        viewModel.onFormatSelect(GameFormat.TEAMS_2V2)
        // Swap seats 2 and 3 between teams: A = players 1 & 3, B = players 2 & 4.
        viewModel.onTeamSelect(1, Team.B)
        viewModel.onTeamSelect(2, Team.A)

        viewModel.onStartGame(listOf("P1", "P2", "P3", "P4"), "XP")

        val config = games.allGames.single().config
        assertEquals(GameFormat.TEAMS_2V2, config.format)
        assertEquals(listOf(SideId("team_a"), SideId("team_b")), config.sides.map { it.id })
        assertEquals(
            listOf("P1", "P3"),
            config.sides[0].playerIds.map { id ->
                config.players.first { it.id == id }.name
            },
        )
        assertEquals(
            listOf("P2", "P4"),
            config.sides[1].playerIds.map { id ->
                config.players.first { it.id == id }.name
            },
        )
        assertEquals(11, config.targetScore)
    }

    @Test
    fun startGame_fourPlayers_buildsOneSidePerPlayerInSeatOrder() = runTest {
        viewModel.onFormatSelect(GameFormat.FOUR_PLAYERS)

        viewModel.onStartGame(listOf("P1", "P2", "P3", "P4"), "XP")

        val config = games.allGames.single().config
        assertEquals(4, config.sides.size)
        assertEquals(listOf("P1", "P2", "P3", "P4"), config.players.map { it.name })
        config.sides.zip(config.players).forEach { (side, player) -> assertEquals(listOf(player.id), side.playerIds) }
    }

    @Test
    fun shrinkingFormat_keepsFirstPlayers_andTeamsResetWhenReturningTo2v2() = runTest {
        viewModel.onFormatSelect(GameFormat.FOUR_PLAYERS)
        viewModel.onNameChange(0, "Ana")
        viewModel.onNameChange(3, "Dan")
        viewModel.onFormatSelect(GameFormat.TEAMS_2V2)
        viewModel.onTeamSelect(0, Team.B)
        viewModel.onFormatSelect(GameFormat.FOUR_PLAYERS)
        viewModel.onFormatSelect(GameFormat.TEAMS_2V2)
        viewModel.onFormatSelect(GameFormat.TWO_PLAYERS)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(listOf("Ana", ""), state.players.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.onFormatSelect(GameFormat.TEAMS_2V2)
        viewModel.uiState.test {
            assertEquals(listOf(Team.A, Team.A, Team.B, Team.B), expectMostRecentItem().players.map { it.team })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun newSeats_getColorsNobodyUses() = runTest {
        viewModel.onColorSelect(0, PlayerColors.forSeat(2).id)
        viewModel.onFormatSelect(GameFormat.FOUR_PLAYERS)

        viewModel.uiState.test {
            val colors = expectMostRecentItem().players.map { it.colorId }
            assertEquals(4, colors.distinct().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun savePlayer_storesPreset_andLoadFillsAnotherSeat() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onNameChange(0, "Ana")
            viewModel.onColorSelect(0, "green")
            viewModel.onLegendPickerOpen(0)
            viewModel.onLegendSelect("jinx")
            viewModel.onPlayerSave(0)
            viewModel.onPlayerSave(1) // blank name: ignored

            val saved = expectMostRecentItem().presets.single()
            assertEquals("Ana", saved.name)

            viewModel.onPresetApply(1, saved.id)
            val second = expectMostRecentItem().players[1]
            assertEquals("Ana", second.name)
            assertEquals("green", second.colorId)
            assertEquals("jinx", second.legend?.id)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, presets.current.size)
    }

    @Test
    fun loadedPreset_isLinkedInConfig_unlessTheNameChanges() = runTest {
        val presets = FakePlayerPresetRepository(listOf(PlayerPreset("ana", "Ana", "green", "jinx")))
        val viewModel = SetupViewModel(games, legends, settings, presets, picker, timers)
        viewModel.uiState.test {
            @Suppress("ControlFlowWithEmptyBody")
            while (awaitItem().presets.isEmpty()) {}
            viewModel.onPresetApply(0, "ana")
            viewModel.onPresetApply(1, "ana")
            viewModel.onColorSelect(0, "pink") // a different color keeps the link
            viewModel.onNameChange(0, "An") // editing back to the same name keeps it
            viewModel.onNameChange(0, " ana ")
            viewModel.onNameChange(1, "Ann") // a different name breaks it
            expectMostRecentItem()
            viewModel.onStartGame(defaultNames, "XP")
            cancelAndIgnoreRemainingEvents()
        }

        val players = games.allGames.single().config.players
        assertEquals(listOf("ana", null), players.map { it.presetId })
    }

    @Test
    fun typedPlayer_hasNoPresetId() = runTest {
        viewModel.onNameChange(0, "Ana")
        viewModel.onStartGame(defaultNames, "XP")

        assertEquals(listOf(null, null), games.allGames.single().config.players.map { it.presetId })
    }

    @Test
    fun savedSeat_isLinkedInConfig_withTheSavedId() = runTest {
        viewModel.onNameChange(0, "Ana")
        viewModel.onPlayerSave(0)
        viewModel.onStartGame(defaultNames, "XP")

        val savedId = presets.current.single().id
        assertEquals(listOf(savedId, null), games.allGames.single().config.players.map { it.presetId })
    }

    @Test
    fun savedSeat_renamedAfterwards_isUnlinked() = runTest {
        viewModel.onNameChange(0, "Ana")
        viewModel.onPlayerSave(0)
        viewModel.onNameChange(0, "Ann")
        viewModel.onStartGame(defaultNames, "XP")

        assertEquals(listOf(null, null), games.allGames.single().config.players.map { it.presetId })
    }

    @Test
    fun savingAnExistingName_reusesThatPreset() = runTest {
        val presets = FakePlayerPresetRepository(listOf(PlayerPreset("ana", "Ana", "green", "jinx")))
        val viewModel = SetupViewModel(games, legends, settings, presets, picker, timers)
        viewModel.onNameChange(0, "ana")
        viewModel.onColorSelect(0, "pink")
        viewModel.onPlayerSave(0)
        viewModel.onStartGame(defaultNames, "XP")

        assertEquals(listOf(PlayerPreset("ana", "Ana", "pink", null)), presets.current)
        assertEquals("ana", games.allGames.single().config.players[0].presetId)
    }

    @Test
    fun roundTimer_defaultsToOff50() = runTest {
        viewModel.uiState.test {
            assertEquals(RoundTimerSetupUiState(enabled = false, minutes = 50), awaitItem().roundTimer)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun roundTimer_toggleAndStepper_areRemembered_andClamped() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.onRoundTimerToggle(true)
            viewModel.onRoundTimerDecrease()
            assertEquals(RoundTimerSetupUiState(enabled = true, minutes = 45), expectMostRecentItem().roundTimer)
            assertEquals(RoundTimerChoice(enabled = true, minutes = 45), timers.currentChoice)

            repeat(20) { viewModel.onRoundTimerDecrease() }
            val atMin = expectMostRecentItem().roundTimer
            assertEquals(5, atMin.minutes)
            assertFalse(atMin.canDecrease)
            assertTrue(atMin.canIncrease)

            repeat(30) { viewModel.onRoundTimerIncrease() }
            val atMax = expectMostRecentItem().roundTimer
            assertEquals(120, atMax.minutes)
            assertFalse(atMax.canIncrease)

            // Off keeps the length for next time.
            viewModel.onRoundTimerToggle(false)
            assertEquals(
                RoundTimerSetupUiState(enabled = false, minutes = 120, canIncrease = false),
                expectMostRecentItem().roundTimer,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun startGame_withRoundTimer_createsAFreshPausedTimer_beforeOpening() = runTest {
        timers.setChoiceEnabled(true)
        timers.adjustChoiceMinutes(-20) // 30 minutes
        viewModel.navigationEvents.test {
            viewModel.onStartGame(defaultNames, "XP")
            val gameId = (awaitItem() as SetupNavigationEvent.OpenTracker).gameId
            assertEquals(RoundTimer(durationMs = 30 * 60_000L), timers.allTimers[gameId])
        }
    }

    @Test
    fun startGame_withRoundTimerOff_createsNoTimer() = runTest {
        viewModel.onStartGame(defaultNames, "XP")

        assertEquals(1, games.allGames.size)
        assertTrue(timers.allTimers.isEmpty())
    }

    @Test
    fun blankName_getsPlaceholder_andIsMarkedDefault() = runTest {
        viewModel.onNameChange(0, "Ana")
        viewModel.onNameChange(1, "   ")
        viewModel.onStartGame(defaultNames, "XP")

        val players = games.allGames.single().config.players
        assertEquals(listOf("Ana", "Player 2"), players.map { it.name })
        assertEquals(listOf(false, true), players.map { it.hasDefaultName })
    }
}
