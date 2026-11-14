package com.rifttally.feature.history

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.ui.R as CoreUiR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Semantics tests of the stateless game detail screen (JVM, Robolectric). */
@RunWith(RobolectricTestRunner::class)
class GameDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun showsScoreboardDurationChartAndLog() {
        composeRule.setContent {
            RiftTheme { GameDetailScreen(uiState = previewDetailState, onBackClick = {}) }
        }

        composeRule.onNodeWithText(context.getString(R.string.feature_history_winner, "Ana")).assertExists()
        val duration = context.resources.getQuantityString(R.plurals.feature_history_duration_minutes, 27, 27)
        composeRule.onNodeWithText(context.getString(R.string.feature_history_duration, duration)).assertExists()

        val finalScores = listOf(
            context.resources.getQuantityString(R.plurals.feature_history_side_points, 8, "Ana", 8),
            context.resources.getQuantityString(R.plurals.feature_history_side_points, 5, "Ben", 5),
        ).joinToString(", ")
        val list = composeRule.onNode(hasScrollAction())
        val chart = context.getString(R.string.feature_history_chart_description, finalScores)
        list.performScrollToNode(hasContentDescription(chart))
        composeRule.onNodeWithContentDescription(chart).assertExists()

        val ended = context.getString(CoreUiR.string.core_ui_log_ended_winner, "Ana")
        list.performScrollToNode(hasText(ended))
        composeRule.onNodeWithText(ended).assertExists()
        composeRule.onNodeWithText(context.getString(CoreUiR.string.core_ui_log_counter, "Ben", "XP", 2, 2))
            .assertExists()
    }

    @Test
    fun notFound_showsMessage() {
        composeRule.setContent {
            RiftTheme { GameDetailScreen(uiState = GameDetailUiState.NotFound, onBackClick = {}) }
        }

        composeRule.onNodeWithText(context.getString(R.string.feature_history_not_found)).assertExists()
    }
}
