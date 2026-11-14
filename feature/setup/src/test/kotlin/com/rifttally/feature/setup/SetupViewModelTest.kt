package com.rifttally.feature.setup

import app.cash.turbine.test
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
import com.rifttally.core.model.Legend
import com.rifttally.core.model.SideId
import com.rifttally.core.testing.MainDispatcherRule
import com.rifttally.core.testing.repository.FakeGameRepository
import com.rifttally.core.testing.repository.FakeLegendRepository
import com.rifttally.core.testing.repository.FakeUserSettingsRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SetupViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val games = FakeGameRepository()
    private val settings = FakeUserSettingsRepository()
    private val legends = FakeLegendRepository(
        listOf(Legend("jinx", "Jinx", null), Legend("viktor", "Viktor", null), Legend("lee_sin", "Lee Sin", null)),
    )

    // Lazy: created inside each test, after MainDispatcherRule has installed the test Main dispatcher.
    private val viewModel by lazy { SetupViewModel(games, legends, settings) }

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
}
