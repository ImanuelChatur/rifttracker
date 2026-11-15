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
 * The control strip with a round timer: normal (paused), last 5 minutes (warning color) and overtime
 * (error color, counting up). Records `src/test/screenshots/tracker_timer_<case>.png`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
class TrackerTimerScreenshotTest(private val case: Case) {

    data class Case(val name: String, val panelCount: Int, val timer: Timer, val darkTheme: Boolean) {
        override fun toString(): String = "tracker_timer_${name}_${if (darkTheme) "dark" else "light"}.png"
    }

    /** The timer states to capture (public, for the parameterized runner). */
    enum class Timer { Normal, Warning, Overtime }

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun capture() {
        val state = previewTrackerState.copy(
            panels = (0 until case.panelCount).map { index ->
                SidePanelUiState(
                    sideId = SideId("s$index"),
                    points = index * 3,
                    isAtTarget = false,
                    players = persistentListOf(
                        PanelPlayerUiState(
                            playerId = PlayerId("p$index"),
                            name = NAMES[index],
                            colorId = COLORS[index],
                            legendName = null,
                            legendImageUri = null,
                        ),
                    ),
                )
            }.toImmutableList(),
        )
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
                    timer = timerStates.getValue(case.timer),
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(case.toString())
    }

    companion object {
        private val NAMES = listOf("Ana", "Ben", "Cleo", "Dan")
        private val COLORS = listOf("orange", "sky", "green", "pink")
        private val timerStates = mapOf(
            Timer.Normal to RoundTimerUiState(isRunning = false, seconds = 3_000, phase = RoundTimerPhase.Normal),
            Timer.Warning to RoundTimerUiState(isRunning = true, seconds = 272, phase = RoundTimerPhase.Warning),
            Timer.Overtime to RoundTimerUiState(isRunning = true, seconds = 133, phase = RoundTimerPhase.Overtime),
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Case> = listOf(
            Case("two_normal", panelCount = 2, timer = Timer.Normal, darkTheme = true),
            Case("two_normal", panelCount = 2, timer = Timer.Normal, darkTheme = false),
            Case("two_warning", panelCount = 2, timer = Timer.Warning, darkTheme = true),
            Case("two_warning", panelCount = 2, timer = Timer.Warning, darkTheme = false),
            Case("two_overtime", panelCount = 2, timer = Timer.Overtime, darkTheme = true),
            Case("two_overtime", panelCount = 2, timer = Timer.Overtime, darkTheme = false),
            // The strip on a narrower seat (top of the middle panel) and on the 4-player seam.
            Case("three_warning", panelCount = 3, timer = Timer.Warning, darkTheme = true),
            Case("four_overtime", panelCount = 4, timer = Timer.Overtime, darkTheme = true),
        )
    }
}
