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
import kotlin.test.assertNull
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
    private val legends = FakeLegendRepository(listOf(Legend("jinx", "Jinx", null), Legend("viktor", "Viktor", null)))

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
        viewModel.onPickFirstPlayerToggle(false)

        val current = settings.current
        assertEquals(ThemeMode.LIGHT, current.themeMode)
        assertEquals(true, current.useDynamicColor)
        assertEquals(false, current.hapticsEnabled)
        assertEquals(false, current.keepScreenOn)
        assertEquals(false, current.immersiveMode)
        assertEquals(GameFormat.TEAMS_2V2, current.defaultGameFormat)
        assertEquals(true, current.addXpCounterByDefault)
        assertEquals(false, current.pickFirstPlayer)
    }

    @Test
    fun presetDelete_removesIt() = runTest {
        viewModel.onPresetDelete("1")

        assertEquals(listOf("Ana"), presets.current.map { it.name })
    }

    @Test
    fun presetEdit_draftsChanges_andSaveKeepsTheId() = runTest {
        viewModel.uiState.test {
            viewModel.onPresetEdit("2")
            viewModel.onPresetNameChange("Anna")
            viewModel.onPresetColorSelect("green")
            viewModel.onPresetLegendPickerOpen()
            viewModel.onPresetLegendQueryChange("vik")

            val editing = assertIs<SettingsUiState.Success>(expectMostRecentItem()).presetEditor
            assertEquals("Anna", editing?.name)
            assertEquals(listOf("Viktor"), editing?.legendPicker?.legends?.map { it.name })
            // Nothing is written before "Save".
            assertEquals(PlayerPreset("2", "Ana", "orange", "jinx"), presets.current.single { it.id == "2" })

            viewModel.onPresetLegendSelect("viktor")
            viewModel.onPresetEditSave()

            val saved = assertIs<SettingsUiState.Success>(expectMostRecentItem())
            assertNull(saved.presetEditor)
            assertEquals(PlayerPreset("2", "Anna", "green", "viktor"), presets.current.single { it.id == "2" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun presetEdit_legendNone_andDismissDiscards() = runTest {
        viewModel.uiState.test {
            viewModel.onPresetEdit("2")
            viewModel.onPresetLegendPickerOpen()
            viewModel.onPresetLegendSelect(null)

            val editing = assertIs<SettingsUiState.Success>(expectMostRecentItem()).presetEditor
            assertNull(editing?.legend)
            assertNull(editing?.legendPicker)

            viewModel.onPresetEditDismiss()

            assertNull(assertIs<SettingsUiState.Success>(expectMostRecentItem()).presetEditor)
            assertEquals("jinx", presets.current.single { it.id == "2" }.legendId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun presetEdit_blankName_cannotSave_andLongNameIsCut() = runTest {
        viewModel.uiState.test {
            viewModel.onPresetEdit("1")
            viewModel.onPresetNameChange("   ")
            viewModel.onPresetEditSave()

            val editing = assertIs<SettingsUiState.Success>(expectMostRecentItem()).presetEditor
            assertEquals(false, editing?.canSave)
            assertEquals("Ben", presets.current.single { it.id == "1" }.name)

            viewModel.onPresetNameChange("x".repeat(40))
            assertEquals(24, assertIs<SettingsUiState.Success>(expectMostRecentItem()).presetEditor?.name?.length)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun presetEdit_unknownId_doesNothing() = runTest {
        viewModel.uiState.test {
            viewModel.onPresetEdit("nope")

            assertNull(assertIs<SettingsUiState.Success>(expectMostRecentItem()).presetEditor)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun presetEdit_renameToAnotherPresetsName_showsError_andDoesNotSave() = runTest {
        viewModel.uiState.test {
            viewModel.onPresetEdit("1")
            viewModel.onPresetNameChange(" ana ")
            viewModel.onPresetEditSave()

            val editing = assertIs<SettingsUiState.Success>(expectMostRecentItem()).presetEditor
            assertEquals("Ana", editing?.duplicateName)
            assertEquals("Ben", presets.current.single { it.id == "1" }.name)

            viewModel.onPresetNameChange("Benny")
            assertNull(assertIs<SettingsUiState.Success>(expectMostRecentItem()).presetEditor?.duplicateName)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
