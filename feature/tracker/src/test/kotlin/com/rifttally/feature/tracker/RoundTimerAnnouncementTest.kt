package com.rifttally.feature.tracker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import com.rifttally.core.designsystem.theme.RiftTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The timer's live region speaks only at 5:00 and 0 reached on screen, not when a game opens under 5:00. */
@RunWith(RobolectricTestRunner::class)
class RoundTimerAnnouncementTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private var timer by mutableStateOf(timer(phase = RoundTimerPhase.Normal, seconds = 301))

    private fun timer(phase: RoundTimerPhase, seconds: Long) =
        RoundTimerUiState(isRunning = true, seconds = seconds, phase = phase)

    private fun setContent(initial: RoundTimerUiState) {
        timer = initial
        composeRule.setContent {
            RiftTheme { RoundTimerButton(timer = timer, hapticsEnabled = false, onToggle = {}) }
        }
    }

    private fun announcement() = composeRule.onNodeWithTag(ROUND_TIMER_ANNOUNCEMENT_TEST_TAG, useUnmergedTree = true)

    private val silent = SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription)

    @Test
    fun openingUnderFiveMinutes_isSilent() {
        setContent(timer(RoundTimerPhase.Warning, seconds = 180))
        announcement().assert(silent)

        timer = timer(RoundTimerPhase.Warning, seconds = 179) // a normal tick
        composeRule.waitForIdle()
        announcement().assert(silent)
    }

    @Test
    fun openingInOvertime_isSilent() {
        setContent(timer(RoundTimerPhase.Overtime, seconds = 40))
        announcement().assert(silent)
    }

    @Test
    fun reachingFiveMinutes_thenZero_onScreen_isAnnounced() {
        setContent(timer(RoundTimerPhase.Normal, seconds = 301))
        announcement().assert(silent)

        timer = timer(RoundTimerPhase.Warning, seconds = 300)
        composeRule.waitForIdle()
        announcement().assertContentDescriptionEquals(
            context.getString(R.string.feature_tracker_timer_five_minutes_left),
        )

        timer = timer(RoundTimerPhase.Overtime, seconds = 0)
        composeRule.waitForIdle()
        announcement().assertContentDescriptionEquals(context.getString(R.string.feature_tracker_timer_up))
    }

    @Test
    fun crossingSeenLate_afterTheScreenWasOff_isSilent() {
        setContent(timer(RoundTimerPhase.Normal, seconds = 900))

        // Screen off at 15:00, back on at 2:00 left: the warning is stale news.
        timer = timer(RoundTimerPhase.Warning, seconds = 120)
        composeRule.waitForIdle()
        announcement().assert(silent)

        // ...and 10 minutes into overtime.
        timer = timer(RoundTimerPhase.Overtime, seconds = 600)
        composeRule.waitForIdle()
        announcement().assert(silent)
    }

    @Test
    fun afterAReset_reachingTheOpeningPhaseAgain_isAnnounced() {
        setContent(timer(RoundTimerPhase.Warning, seconds = 120))

        timer = timer(RoundTimerPhase.Normal, seconds = 3_000) // reset
        composeRule.waitForIdle()
        announcement().assert(silent)

        timer = timer(RoundTimerPhase.Warning, seconds = 300)
        composeRule.waitForIdle()
        announcement().assertContentDescriptionEquals(
            context.getString(R.string.feature_tracker_timer_five_minutes_left),
        )
    }
}
