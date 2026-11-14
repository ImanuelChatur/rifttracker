package com.rifttally.feature.setup

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.Legend
import com.rifttally.core.ui.LEGEND_ART_TEST_TAG
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Setup with the Legend picker sheet open (the sheet is its own window, so the whole screen is captured).
 * Records `src/test/screenshots/setup_picker_*.png`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
class SetupScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pickerOpen_dark() = capture(darkTheme = true, name = "setup_picker_dark.png")

    @Test
    fun pickerOpen_light() = capture(darkTheme = false, name = "setup_picker_light.png")

    private fun capture(darkTheme: Boolean, name: String) {
        val arts = listOf(TestArt.Warm, TestArt.Cool, TestArt.Bright)
        // Every other Legend has (generated) art, so both the art and the fallback cards show.
        val legends = listOf("Ahri", "Annie", "Darius", "Ezreal", "Jinx", "Kai'Sa", "Lee Sin", "Miss Fortune", "Viktor")
            .mapIndexed { index, legendName ->
                val id = legendName.lowercase().replace(Regex("[^a-z]+"), "_")
                Legend(id, legendName, imageUri = if (index % 2 == 0) arts[index % arts.size].uri(256, 256) else null)
            }
        val picker = LegendPickerUiState(
            playerIndex = 0,
            query = "",
            legends = legends.toPersistentList(),
            selectedLegendId = "jinx",
        )
        val state = previewSetupState.copy(legendPicker = picker)
        composeRule.setContent {
            RiftTheme(darkTheme = darkTheme) {
                SetupScreen(
                    uiState = state,
                    onBackClick = {},
                    onNameChange = { _, _ -> },
                    onColorSelect = { _, _ -> },
                    onFormatSelect = {},
                    onTeamSelect = { _, _ -> },
                    onPlayerSave = {},
                    onPresetApply = { _, _ -> },
                    onLegendClick = {},
                    onTargetIncrease = {},
                    onTargetDecrease = {},
                    onAddXpCounterToggle = {},
                    onStartClick = {},
                )
                LegendPickerSheet(
                    picker = picker,
                    playerName = "Ana",
                    playerColor = state.players.first().pickerColor(),
                    onQueryChange = {},
                    onLegendSelect = {},
                    onDismiss = {},
                )
            }
        }
        // The grid is lazy: only the first rows (Ahri, Darius, Jinx have art) are composed.
        val expected = VISIBLE_ART_CARDS
        composeRule.waitUntil(timeoutMillis = ART_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(LEGEND_ART_TEST_TAG, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false).size >= expected
        }
        composeRule.waitForIdle()
        captureScreenRoboImage(name)
    }

    private fun <T> List<T>.toPersistentList() = persistentListOf<T>().addAll(this)

    private companion object {
        const val ART_TIMEOUT_MILLIS = 10_000L
        const val VISIBLE_ART_CARDS = 3
    }
}
