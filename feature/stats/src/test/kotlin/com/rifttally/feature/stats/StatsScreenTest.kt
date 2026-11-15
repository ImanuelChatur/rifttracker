package com.rifttally.feature.stats

import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.stats.StatsPeriod
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Semantics tests of the stateless stats screen (JVM, Robolectric). */
@RunWith(RobolectricTestRunner::class)
class StatsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = RuntimeEnvironment.getApplication()

    private val players = mutableListOf<String>()
    private val tabs = mutableListOf<StatsTab>()
    private val sorts = mutableListOf<LegendSort>()
    private val legends = mutableListOf<String>()
    private val periods = mutableListOf<StatsPeriod>()

    private fun setContent(state: StatsUiState) {
        composeRule.setContent {
            RiftTheme {
                StatsScreen(
                    uiState = state,
                    actions = StatsActions(
                        onPlayerClick = { players += it },
                        onLegendClick = { legends += it },
                        onTabSelect = { tabs += it },
                        onLegendSortSelect = { sorts += it },
                        onPeriodSelect = { periods += it },
                    ),
                )
            }
        }
    }

    @Test
    fun periodChips_selectARange_andTheHeroNamesIt() {
        setContent(previewStatsState.copy(period = StatsPeriod.THIS_YEAR))

        val thisYear = context.getString(R.string.feature_stats_period_this_year)
        composeRule.onNode(hasText(thisYear) and isSelected()).assertExists()
        val label = context.getString(
            R.string.feature_stats_label_with_period,
            context.getString(R.string.feature_stats_total_games_label),
            thisYear,
        )
        composeRule.onNodeWithText(label, substring = true).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.feature_stats_period_last_30_days)).performClick()

        assertEquals(listOf(StatsPeriod.LAST_30_DAYS), periods)
    }

    @Test
    fun overview_showsTheFirstPlayerRate() {
        setContent(previewStatsState)

        val text = context.resources.getQuantityString(R.plurals.feature_stats_going_first_wins, 5, 60, 5)
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
        composeRule.onNodeWithText(text).assertExists()
    }

    @Test
    fun emptyPeriod_showsMessage_andGoesBackToAllTime() {
        setContent(StatsUiState.EmptyPeriod(StatsPeriod.LAST_30_DAYS))

        composeRule.onNodeWithText(context.getString(R.string.feature_stats_period_empty)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.feature_stats_period_empty_action)).performClick()

        assertEquals(listOf(StatsPeriod.ALL_TIME), periods)
    }

    @Test
    fun legendRow_opensTheLegend() {
        setContent(previewStatsState.copy(selectedTab = StatsTab.LEGENDS))

        composeRule.onNodeWithText("Viktor").performClick()

        assertEquals(listOf("viktor"), legends)
    }

    @Test
    fun overview_showsTotalsAndRanking_andOpensAPlayer() {
        setContent(previewStatsState)

        composeRule.onNodeWithText("8").assertExists()
        composeRule.onNodeWithText("Jinx").assertExists()
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Ana"))
        composeRule.onAllNodesWithText("Ana").onFirst().performClick()
        assertEquals(listOf("name:ana"), players)

        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Cleo"))
        composeRule.onNodeWithText("Cleo").performClick()
        assertEquals(listOf("name:ana", "name:cleo"), players)
    }

    @Test
    fun tabs_andLegendSort_invokeCallbacks() {
        setContent(previewStatsState.copy(selectedTab = StatsTab.LEGENDS))

        composeRule.onNodeWithText("Viktor").assertExists()
        composeRule.onNodeWithText(context.getString(R.string.feature_stats_sort_name)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.feature_stats_tab_overview)).performClick()

        assertEquals(listOf(LegendSort.NAME), sorts)
        assertEquals(listOf(StatsTab.OVERVIEW), tabs)
    }

    @Test
    fun empty_showsMessage() {
        setContent(StatsUiState.Empty)

        composeRule.onNodeWithText(context.getString(R.string.feature_stats_empty)).assertExists()
    }
}
