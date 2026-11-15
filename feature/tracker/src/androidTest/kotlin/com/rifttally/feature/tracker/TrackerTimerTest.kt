package com.rifttally.feature.tracker

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rifttally.core.designsystem.theme.RiftTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackerTimerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var toggles = 0
    private var resets = 0

    private fun setContent(timer: RoundTimerUiState?) {
        composeTestRule.setContent {
            RiftTheme {
                TrackerScreen(
                    uiState = previewTrackerState,
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
                    timer = timer,
                    onTimerToggle = { toggles++ },
                    onTimerReset = { resets++ },
                )
            }
        }
    }

    @Test
    fun timer_isLabeledWithTimeLeft_andTapToggles() {
        setContent(RoundTimerUiState(isRunning = false, seconds = 750, phase = RoundTimerPhase.Normal))

        composeTestRule.onNodeWithTag(ROUND_TIMER_TEST_TAG)
            .assertContentDescriptionEquals("12 minutes 30 seconds left in round")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Paused"))
            .performClick()

        assertEquals(1, toggles)
    }

    @Test
    fun overtime_isDescribedAsOverTime() {
        setContent(RoundTimerUiState(isRunning = true, seconds = 133, phase = RoundTimerPhase.Overtime))

        composeTestRule.onNodeWithTag(ROUND_TIMER_TEST_TAG)
            .assertContentDescriptionEquals("2 minutes 13 seconds over time")
    }

    @Test
    fun menu_resetTimer_onlyWithATimer() {
        setContent(RoundTimerUiState(isRunning = true, seconds = 60, phase = RoundTimerPhase.Warning))

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.feature_tracker_menu)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.feature_tracker_menu_reset_timer)).performClick()

        assertEquals(1, resets)
    }

    @Test
    fun noTimer_showsNoTimer() {
        setContent(timer = null)

        composeTestRule.onNodeWithTag(ROUND_TIMER_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.feature_tracker_menu)).performClick()
        composeTestRule.onNodeWithText(
            context.getString(R.string.feature_tracker_menu_reset_timer),
        ).assertDoesNotExist()
    }
}
