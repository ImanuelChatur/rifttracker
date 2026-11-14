package com.rifttally.feature.settings

import app.cash.turbine.test
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.Legend
import com.rifttally.core.model.PlayerPreset
import com.rifttally.core.model.ThemeMode
import com.rifttally.core.testing.MainDispatcherRule
import com.rifttally.core.testing.repository.FakeLegendRepository
import com.rifttally.core.testing.repository.FakePlayerPresetRepository
import com.rifttally.core.testing.repository.FakeUserSettingsRepository
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settings = FakeUserSettingsRepository()
    private val presets = FakePlayerPresetRepository(
        listOf(PlayerPreset("1", "Ben", "sky", null), PlayerPreset("2", "Ana", "orange", "jinx")),
    )
    private val legends = FakeLegendRepository(listOf(Legend("jinx", "Jinx", null)))

    // Lazy: created inside each test, after MainDispatcherRule has installed the test Main dispatcher.
    private val viewModel by lazy { SettingsViewModel(settings, presets, legends) }

    @Test
    fun state_reflectsSettings_andPresetsWithLegendNames() = runTest {
        viewModel.uiState.test {
            val state = assertIs<SettingsUiState.Success>(expectMostRecentItem())
            assertEquals(ThemeMode.DARK, state.themeMode)
            assertEquals(listOf("Ana" to "Jinx", "Ben" to null), state.presets.map { it.name to it.legendName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun everyAction_isPersisted() = runTest {
        viewModel.onThemeModeSelect(ThemeMode.LIGHT)
        viewModel.onDynamicColorToggle(true)
        viewModel.onHapticsToggle(false)
        viewModel.onKeepScreenOnToggle(false)
        viewModel.onImmersiveModeToggle(false)
        viewModel.onDefaultFormatSelect(GameFormat.TEAMS_2V2)
        viewModel.onAddXpByDefaultToggle(true)

        val current = settings.current
        assertEquals(ThemeMode.LIGHT, current.themeMode)
        assertEquals(true, current.useDynamicColor)
        assertEquals(false, current.hapticsEnabled)
        assertEquals(false, current.keepScreenOn)
        assertEquals(false, current.immersiveMode)
        assertEquals(GameFormat.TEAMS_2V2, current.defaultGameFormat)
        assertEquals(true, current.addXpCounterByDefault)
    }

    @Test
    fun presetDelete_removesIt() = runTest {
        viewModel.onPresetDelete("1")

        assertEquals(listOf("Ana"), presets.current.map { it.name })
    }
}
