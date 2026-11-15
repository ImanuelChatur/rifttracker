package com.rifttally.feature.tracker

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.SideId
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The first-player reveal once it has settled ("<Name> goes first" facing the chosen seat) and the "1st"
 * marker. Records `src/test/screenshots/tracker_first_<case>.png` with `recordRoborazziDebug`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
class TrackerFirstPlayerScreenshotTest(private val case: Case) {

    /** [chosen] is the panel of the player who goes first; [reveal] false shows only the marker. */
    data class Case(
        val name: String,
        val panelCount: Int,
        val chosen: Int,
        val reveal: Boolean,
        val darkTheme: Boolean,
    ) {
        override fun toString(): String = "tracker_first_${name}_${if (darkTheme) "dark" else "light"}.png"
    }

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun capture() {
        val state = state(case)
        composeRule.setContent {
            RiftTheme(darkTheme = case.darkTheme) {
                TrackerScreen(
                    uiState = state,
                    onBackClick = {},
                    onPointsChange = { _, _ -> },
                    onUndo = {},
                    onTargetScoreChange = {},
                    onEndGame = {},
                    onKeepPlaying = {},
                    onRestart = {},
                    onCounterAdd = { _, _ -> },
                    onCounterChange = { _, _, _ -> },
                    onCounterRemove = { _, _ -> },
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(case.toString())
    }

    companion object {
        private val names = listOf("Ana", "Ben", "Cleo", "Dan")
        private val colors = listOf("orange", "sky", "green", "pink")

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Case> = listOf(
            // The far seat (rotated 180°): the card must read upside down from the bottom.
            Case("two_reveal", panelCount = 2, chosen = 0, reveal = true, darkTheme = true),
            Case("two_reveal", panelCount = 2, chosen = 0, reveal = true, darkTheme = false),
            // A sideways seat.
            Case("four_reveal", panelCount = 4, chosen = 1, reveal = true, darkTheme = true),
            Case("three_marker", panelCount = 3, chosen = 2, reveal = false, darkTheme = false),
            Case("teams_marker", panelCount = 2, chosen = 1, reveal = false, darkTheme = true),
        )

        private fun state(case: Case): TrackerUiState.Success {
            val teams = case.name.startsWith("teams")
            val panels = (0 until case.panelCount).map { index ->
                val members = if (teams) listOf(index, index + 2) else listOf(index)
                SidePanelUiState(
                    sideId = SideId("s$index"),
                    points = 0,
                    isAtTarget = false,
                    players = members.map { player ->
                        PanelPlayerUiState(
                            playerId = PlayerId("p$player"),
                            name = names[player],
                            colorId = colors[player],
                            legendName = null,
                            legendImageUri = null,
                        )
                    }.toImmutableList(),
                    firstPlayerName = names[case.chosen].takeIf { index == case.chosen },
                )
            }
            return previewTrackerState.copy(
                panels = panels.toImmutableList(),
                canUndo = true,
                history = persistentListOf(),
                firstPlayerReveal = if (case.reveal) {
                    FirstPlayerRevealUiState("pick", case.chosen, names[case.chosen], animate = false)
                } else {
                    null
                },
            )
        }
    }
}
