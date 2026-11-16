package com.rifttally.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End to end through the real app (real Hilt graph and Room database): set up a 2-player game,
 * dismiss the first-player reveal, score, undo, leave and resume it from Home, end it (Game over) and
 * go Home.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class TwoPlayerGameFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val activity get() = composeTestRule.activity

    private fun string(id: Int, vararg args: Any): String = activity.getString(id, *args)

    @Test
    fun playResumeAndEndGame() {
        val player1 = string(com.rifttally.feature.setup.R.string.feature_setup_default_player_name, 1)

        // The tap zone's label includes the current score ("Player 1, 0 points, increase").
        fun increase(points: Int): String = activity.resources.getQuantityString(
            com.rifttally.feature.tracker.R.plurals.feature_tracker_increase,
            points,
            player1,
            points,
        )

        // Home → Setup → Tracker.
        composeTestRule.onNodeWithText(string(com.rifttally.feature.home.R.string.feature_home_new_game))
            .performClick()
        composeTestRule.onNodeWithText(string(com.rifttally.feature.setup.R.string.feature_setup_start))
            .performClick()

        // "Pick first player" is on by default: the reveal covers the table (and hides it from
        // accessibility) until tapped away, so wait for it before looking for the panels.
        val goesFirst = hasContentDescription(" goes first", substring = true)
        composeTestRule.waitUntilExactlyOneExists(goesFirst, TIMEOUT_MILLIS)
        composeTestRule.onNode(goesFirst).performClick()
        composeTestRule.waitUntilDoesNotExist(goesFirst, TIMEOUT_MILLIS)
        composeTestRule.waitUntilExactlyOneExists(hasContentDescription(increase(0)), TIMEOUT_MILLIS)

        // Score twice, undo once: 1 point stored.
        composeTestRule.onNodeWithContentDescription(increase(0)).performClick()
        composeTestRule.waitUntilExactlyOneExists(hasContentDescription(increase(1)), TIMEOUT_MILLIS)
        composeTestRule.onNodeWithContentDescription(increase(1)).performClick()
        composeTestRule.waitUntilExactlyOneExists(hasContentDescription(increase(2)), TIMEOUT_MILLIS)
        composeTestRule.onNodeWithContentDescription(
            string(com.rifttally.feature.tracker.R.string.feature_tracker_undo),
        )
            .performClick()
        composeTestRule.waitUntilExactlyOneExists(hasContentDescription(increase(1)), TIMEOUT_MILLIS)

        // Leave and resume from Home.
        composeTestRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        val resume = string(com.rifttally.feature.home.R.string.feature_home_resume)
        composeTestRule.waitUntilAtLeastOneExists(hasText(resume), TIMEOUT_MILLIS)
        composeTestRule.onNodeWithText(resume).performClick()
        composeTestRule.waitUntilExactlyOneExists(hasContentDescription(increase(1)), TIMEOUT_MILLIS)

        // End from the menu: Game over, then Home with no game to resume.
        composeTestRule.onNodeWithContentDescription(
            string(com.rifttally.feature.tracker.R.string.feature_tracker_menu),
        )
            .performClick()
        composeTestRule.onNodeWithText(string(com.rifttally.feature.tracker.R.string.feature_tracker_menu_end))
            .performClick()
        composeTestRule.onNodeWithText(string(com.rifttally.feature.tracker.R.string.feature_tracker_end_dialog_title))
            .assertIsDisplayed()
        // The menu is closed now, so the dialog's confirm button is the only "End game".
        composeTestRule.onNodeWithText(string(com.rifttally.feature.tracker.R.string.feature_tracker_menu_end))
            .performClick()
        val home = string(com.rifttally.feature.tracker.R.string.feature_tracker_game_over_home)
        composeTestRule.waitUntilExactlyOneExists(hasText(home), TIMEOUT_MILLIS)
        composeTestRule.onNodeWithText(home).performClick()
        composeTestRule.waitUntilAtLeastOneExists(
            hasText(string(com.rifttally.feature.home.R.string.feature_home_new_game)),
            TIMEOUT_MILLIS,
        )
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
