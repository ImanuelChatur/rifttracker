package com.rifttally.feature.tracker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.CounterId
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.SideId
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Seat-facing popups: the counter "Remove" button (in place, so it turns with its panel) and the
 * add-counter dialog (its own window, turned to the asking player's seat). Records
 * `src/test/screenshots/tracker_popup_*.png`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
class TrackerPopupsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun counterLabelTap_armsRemoveInPlace_andTapRemoves() {
        val removed = mutableListOf<Pair<PlayerId, CounterId>>()
        setTracker(panelCount = 2, onCounterRemove = { player, counter -> removed += player to counter })

        composeRule.onNodeWithContentDescription("Ana, XP 3").performClick()
        val remove = composeRule.onNodeWithContentDescription(
            context.getString(com.rifttally.core.ui.R.string.core_ui_counter_remove_description, "Ana", "XP"),
        )
        remove.assertIsDisplayed()
        captureScreenRoboImage("tracker_popup_remove_far_seat.png")

        remove.performClick()
        assertEquals(listOf(PlayerId("p0") to CounterId("xp")), removed)
    }

    @Test
    fun addCounter_farSeat_dialogFacesThatSeat() {
        setTracker(panelCount = 2)
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.feature_tracker_add_counter_for, "Ana"),
        ).performClick()
        composeRule.onNodeWithText(context.getString(R.string.feature_tracker_counter_dialog_title, "Ana"))
            .assertIsDisplayed()
        composeRule.waitForIdle()
        captureScreenRoboImage("tracker_popup_add_counter_far_seat.png")
    }

    @Test
    fun addCounter_sideSeat_dialogFacesThatSeat() {
        setTracker(panelCount = 4)
        // Seat 3 (Dan) sits on the left in portrait.
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.feature_tracker_add_counter_for, "Dan"),
        ).performClick()
        composeRule.waitForIdle()
        captureScreenRoboImage("tracker_popup_add_counter_side_seat.png")
    }

    private fun setTracker(panelCount: Int, onCounterRemove: (PlayerId, CounterId) -> Unit = { _, _ -> }) {
        val names = listOf("Ana" to "orange", "Ben" to "sky", "Cleo" to "green", "Dan" to "pink")
        val panels = names.take(panelCount).mapIndexed { index, (name, color) ->
            SidePanelUiState(
                sideId = SideId("s$index"),
                points = index + 2,
                isAtTarget = false,
                players = persistentListOf(
                    PanelPlayerUiState(
                        playerId = PlayerId("p$index"),
                        name = name,
                        colorId = color,
                        legendName = null,
                        legendImageUri = null,
                        counters = if (index == 0) {
                            persistentListOf(CounterUiState(CounterId("xp"), "XP", 3))
                        } else {
                            persistentListOf()
                        },
                    ),
                ),
            )
        }.toImmutableList()
        composeRule.setContent {
            RiftTheme(darkTheme = true) {
                TrackerScreen(
                    uiState = previewTrackerState.copy(panels = panels),
                    onBackClick = {},
                    onPointsChange = { _, _ -> },
                    onUndo = {},
                    onTargetScoreChange = {},
                    onEndGame = {},
                    onKeepPlaying = {},
                    onRestart = {},
                    onCounterAdd = { _, _ -> },
                    onCounterChange = { _, _, _ -> },
                    onCounterRemove = onCounterRemove,
                )
            }
        }
    }
}
