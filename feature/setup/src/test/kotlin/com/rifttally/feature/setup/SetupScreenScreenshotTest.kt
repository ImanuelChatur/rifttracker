package com.rifttally.feature.setup

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.Legend
import com.rifttally.core.ui.LEGEND_ART_TEST_TAG
import com.rifttally.core.ui.LegendPickerSheet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Setup: the form itself (2, 3, 4 players, light and dark, 200% font scale, landscape) and with the
 * Legend picker sheet open (the sheet is its own window, so the whole screen is captured).
 * Records `src/test/screenshots/setup_*.png`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
class SetupScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pickerOpen_dark() = capturePicker(darkTheme = true, name = "setup_picker_dark.png")

    @Test
    fun pickerOpen_light() = capturePicker(darkTheme = false, name = "setup_picker_light.png")

    @Test
    fun twoPlayers_dark() = captureForm(twoPlayers(), darkTheme = true, name = "setup_twoplayers_dark.png")

    @Test
    fun twoPlayers_light() = captureForm(twoPlayers(), darkTheme = false, name = "setup_twoplayers_light.png")

    @Test
    fun threePlayers_dark() = captureForm(threePlayers(), darkTheme = true, name = "setup_threeplayers_dark.png")

    @Test
    fun fourPlayers_light() =
        captureForm(previewSetupFourPlayersState, darkTheme = false, "setup_fourplayers_light.png")

    @Test
    fun teams_dark() = captureForm(teams(), darkTheme = true, name = "setup_teams_dark.png")

    // DESIGN.md §5: text wraps and nothing clips at 200% font scale.
    @Test
    @Config(fontScale = 2f)
    fun twoPlayers_fontScale200_dark() =
        captureForm(twoPlayers(), darkTheme = true, name = "setup_twoplayers_font200_dark.png")

    // DESIGN.md §8: landscape phone (compact height), content capped to a centered column.
    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun twoPlayers_landscape_dark() =
        captureForm(twoPlayers(), darkTheme = true, name = "setup_twoplayers_land_dark.png")

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun fourPlayers_landscape_light() =
        captureForm(previewSetupFourPlayersState, darkTheme = false, name = "setup_fourplayers_land_light.png")

    @Test
    fun recentLineups_dark() = captureForm(withLineups(), darkTheme = true, name = "setup_lineups_dark.png")

    @Test
    fun recentLineups_light() = captureForm(withLineups(), darkTheme = false, name = "setup_lineups_light.png")

    @Test
    fun roundTimerOn_dark() =
        captureForm(withTimer(), darkTheme = true, name = "setup_timer_dark.png", scrollToBottom = true)

    @Test
    fun roundTimerOn_light() =
        captureForm(withTimer(), darkTheme = false, name = "setup_timer_light.png", scrollToBottom = true)

    private fun withTimer(): SetupUiState = twoPlayers().copy(
        roundTimer = RoundTimerSetupUiState(enabled = true, minutes = 50),
    )

    private fun withLineups(): SetupUiState = twoPlayers().copy(
        recentLineups = persistentListOf(
            RecentLineupUiState(
                0,
                persistentListOf("Ana", "Ben"),
                persistentListOf("orange", "sky"),
                GameFormat.TWO_PLAYERS,
            ),
            RecentLineupUiState(
                1,
                persistentListOf("Ana", "Ben", "Cleo"),
                persistentListOf("orange", "sky", "green"),
                GameFormat.THREE_PLAYERS,
            ),
            RecentLineupUiState(
                2,
                persistentListOf("Ana", "Ben", "Cleo", "Dan"),
                persistentListOf("orange", "sky", "green", "pink"),
                GameFormat.TEAMS_2V2,
            ),
        ),
    )

    private fun twoPlayers(): SetupUiState = previewSetupState.copy(
        players = previewSetupState.players.mapIndexed { index, player ->
            if (index == 0) player.copy(legend = Legend("jinx", "Jinx", TestArt.Warm.uri(256, 256))) else player
        }.toImmutableList(),
    )

    private fun threePlayers(): SetupUiState = previewSetupFourPlayersState.copy(
        format = GameFormat.THREE_PLAYERS,
        players = previewSetupFourPlayersState.players.take(3).toImmutableList(),
    )

    private fun teams(): SetupUiState = previewSetupFourPlayersState.copy(
        format = GameFormat.TEAMS_2V2,
        players = previewSetupFourPlayersState.players.mapIndexed { index, player ->
            player.copy(team = if (index < 3) Team.A else Team.B)
        }.toImmutableList(),
        teamsInvalid = true,
        canStart = false,
    )

    private fun captureForm(state: SetupUiState, darkTheme: Boolean, name: String, scrollToBottom: Boolean = false) {
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
            }
        }
        val expected = state.players.count { it.legend?.imageUri != null }
        composeRule.waitUntil(timeoutMillis = ART_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(LEGEND_ART_TEST_TAG, useUnmergedTree = true)
                .fetchSemanticsNodes().size >= expected
        }
        // The game options sit below the players: scroll to the last one (the XP toggle).
        if (scrollToBottom) composeRule.onNode(hasText("Add XP counter to all players")).performScrollTo()
        composeRule.waitForIdle()
        captureScreenRoboImage(name)
    }

    private fun capturePicker(darkTheme: Boolean, name: String) {
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
            legends = persistentListOf<Legend>().addAll(legends),
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
                    query = picker.query,
                    legends = picker.legends,
                    selectedLegendId = picker.selectedLegendId,
                    playerName = "Ana",
                    playerColor = state.players.first().pickerColor(),
                    onQueryChange = {},
                    onLegendSelect = {},
                    onDismiss = {},
                )
            }
        }
        // The grid is lazy: only the first rows (Ahri, Darius, Jinx have art) are composed.
        composeRule.waitUntil(timeoutMillis = ART_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(LEGEND_ART_TEST_TAG, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false).size >= VISIBLE_ART_CARDS
        }
        composeRule.waitForIdle()
        captureScreenRoboImage(name)
    }

    private companion object {
        const val ART_TIMEOUT_MILLIS = 10_000L
        const val VISIBLE_ART_CARDS = 3
    }
}
