package com.rifttally.feature.tracker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.CounterId
import com.rifttally.core.model.PlayerId
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
    private val counterAdds = mutableListOf<Pair<PlayerId, String>>()
    private val counterChanges = mutableListOf<Triple<PlayerId, CounterId, Int>>()

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
                    onCounterAdd = { player, label -> counterAdds += player to label },
                    onCounterChange = { player, counter, delta -> counterChanges += Triple(player, counter, delta) },
                    onCounterRemove = { _, _ -> },
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
    fun counterRow_changesValue_andAddCounterDialogAddsXp() {
        setContent(previewTrackerState)

        // Ana has an XP counter in the preview state.
        composeTestRule.onNodeWithContentDescription("Ana, XP, increase").performClick()
        assertEquals(listOf(Triple(PlayerId("p1"), CounterId("xp"), 1)), counterChanges)

        composeTestRule.onNodeWithContentDescription(
            context.getString(R.string.feature_tracker_add_counter_for, "Ben"),
        ).performClick()
        // Ana's existing "XP" counter label is on screen too: target the dialog's preset button.
        composeTestRule.onNode(
            hasText(context.getString(R.string.feature_tracker_counter_preset_xp)) and hasAnyAncestor(isDialog()),
        ).performClick()
        assertEquals(listOf(PlayerId("p2") to "XP"), counterAdds)
    }

    @Test
    fun notFound_offersBack() {
        setContent(TrackerUiState.NotFound)

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.feature_tracker_back)).performClick()

        assertEquals(1, backs)
    }
}
