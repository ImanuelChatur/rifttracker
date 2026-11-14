package com.rifttally.feature.tracker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.SideId
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val changes = mutableListOf<Pair<SideId, Int>>()
    private var undos = 0
    private var ends = 0
    private var keepPlaying = 0
    private var backs = 0

    private fun setContent(uiState: TrackerUiState) {
        composeTestRule.setContent {
            RiftTheme {
                TrackerScreen(
                    uiState = uiState,
                    onBackClick = { backs++ },
                    onPointsChange = { side, delta -> changes += side to delta },
                    onUndo = { undos++ },
                    onTargetScoreChange = {},
                    onEndGame = { ends++ },
                    onKeepPlaying = { keepPlaying++ },
                    onRestart = {},
                )
            }
        }
    }

    @Test
    fun tapZones_changePointsOfThatSide() {
        setContent(previewTrackerState)

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.feature_tracker_increase, "Ana"))
            .performClick()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.feature_tracker_decrease, "Ben"))
            .performClick()

        assertEquals(listOf(SideId("s1") to 1, SideId("s2") to -1), changes)
    }

    @Test
    fun undo_invokesCallback() {
        setContent(previewTrackerState)

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.feature_tracker_undo)).performClick()

        assertEquals(1, undos)
    }

    @Test
    fun winnerBanner_endsGameOrKeepsPlaying() {
        setContent(previewTrackerState.copy(winnerBanner = WinnerBannerUiState(persistentListOf("Ben"))))

        composeTestRule.onNodeWithText(context.getString(R.string.feature_tracker_banner_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.feature_tracker_banner_keep_playing)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.feature_tracker_banner_end)).performClick()

        assertEquals(1, keepPlaying)
        assertEquals(1, ends)
    }

    @Test
    fun menu_opensHistory() {
        setContent(previewTrackerState)

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.feature_tracker_menu)).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.feature_tracker_menu_history)).performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.feature_tracker_history_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Ana +1 · 6").assertIsDisplayed()
    }

    @Test
    fun notFound_offersBack() {
        setContent(TrackerUiState.NotFound)

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.feature_tracker_back)).performClick()

        assertEquals(1, backs)
    }
}
