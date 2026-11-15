package com.rifttally.feature.stats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.stats.StatsPeriod
import kotlin.test.assertEquals
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Semantics tests of the stateless Legend stats screen (JVM, Robolectric). */
@RunWith(RobolectricTestRunner::class)
class LegendStatsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun showsWinRateAndMatchups_withReadableLabels() {
        var backs = 0
        composeRule.setContent {
            RiftTheme { LegendStatsScreen(uiState = previewLegendState, onBackClick = { backs++ }) }
        }

        composeRule.onNodeWithText("Jinx").assertExists() // title
        composeRule.onNodeWithText("67%").assertExists()
        val label = context.getString(
            R.string.feature_stats_label_with_period,
            context.getString(R.string.feature_stats_win_rate_label),
            context.getString(R.string.feature_stats_period_last_30_days),
        )
        composeRule.onNodeWithText(label).assertExists()
        val viktor = context.getString(
            R.string.feature_stats_matchup_description,
            "Viktor",
            context.resources.getQuantityString(R.plurals.feature_stats_games, 4, 4),
            3,
            1,
            context.getString(R.string.feature_stats_win_rate, 75),
        )
        composeRule.onNodeWithContentDescription(viktor).assertExists()

        composeRule.onNodeWithContentDescription(context.getString(R.string.feature_stats_back)).performClick()
        assertEquals(1, backs)
    }

    @Test
    fun noMatchups_saysSo() {
        composeRule.setContent {
            RiftTheme {
                LegendStatsScreen(uiState = previewLegendState.copy(matchups = persistentListOf()), onBackClick = {})
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.feature_stats_matchups_empty)).assertExists()
    }

    @Test
    fun notFound_allTimeOrInAPeriod() {
        var state: LegendStatsUiState by mutableStateOf(LegendStatsUiState.NotFound())
        composeRule.setContent { RiftTheme { LegendStatsScreen(uiState = state, onBackClick = {}) } }

        composeRule.onNodeWithText(context.getString(R.string.feature_stats_legend_not_found)).assertExists()
        state = LegendStatsUiState.NotFound(StatsPeriod.THIS_YEAR)
        composeRule.onNodeWithText(context.getString(R.string.feature_stats_period_empty)).assertExists()
    }
}
