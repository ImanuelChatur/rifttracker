package com.rifttally.feature.tracker

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.SideId
import com.rifttally.core.ui.ResultCardState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/** The Game over overlay. Records `src/test/screenshots/tracker_gameover_<case>.png`. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
class TrackerGameOverScreenshotTest(private val case: Case) {

    /** [winner] is the winning panel, or `null` for no winner. */
    data class Case(
        val name: String,
        val panelCount: Int,
        val teams: Boolean,
        val winner: Int?,
        val darkTheme: Boolean,
    ) {
        override fun toString(): String = "tracker_gameover_${name}_${if (darkTheme) "dark" else "light"}.png"
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
        private val points = listOf(8, 5, 3, 6)

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Case> = listOf(true, false).flatMap { dark ->
            listOf(
                Case("two_winner", panelCount = 2, teams = false, winner = 0, darkTheme = dark),
                Case("four_nowinner", panelCount = 4, teams = false, winner = null, darkTheme = dark),
                Case("teams_winner", panelCount = 2, teams = true, winner = 1, darkTheme = dark),
            )
        }

        private fun state(case: Case): TrackerUiState.Success {
            val panels = (0 until case.panelCount).map { index ->
                val members = if (case.teams) listOf(index, index + 2) else listOf(index)
                SidePanelUiState(
                    sideId = SideId("s$index"),
                    points = points[index],
                    isAtTarget = index == case.winner,
                    players = members.map { player ->
                        PanelPlayerUiState(
                            playerId = PlayerId("p$player"),
                            name = names[player],
                            colorId = colors[player],
                            legendName = null,
                            legendImageUri = null,
                        )
                    }.toImmutableList(),
                )
            }
            return previewTrackerState.copy(
                panels = panels.toImmutableList(),
                history = persistentListOf(),
                gameOver = GameOverUiState(
                    winnerName = case.winner?.let { panels[it].displayName },
                    winnerIsTeam = case.teams,
                    // Present after every real game over: shows the Share button.
                    resultCard = ResultCardState(GameFormat.TWO_PLAYERS, endedAt = 0, sides = persistentListOf()),
                    sides = panels.mapIndexed { index, panel ->
                        GameOverSideUiState(
                            name = panel.displayName,
                            colorId = panel.players.first().colorId,
                            points = panel.points,
                            isWinner = index == case.winner,
                        )
                    }.toImmutableList(),
                ),
            )
        }
    }
}
