package com.rifttally.feature.stats

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.rifttally.core.designsystem.theme.RiftTheme
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

    private fun setContent(state: StatsUiState) {
        composeRule.setContent {
            RiftTheme {
                StatsScreen(
                    uiState = state,
                    actions = StatsActions(
                        onPlayerClick = { players += it },
                        onTabSelect = { tabs += it },
                        onLegendSortSelect = { sorts += it },
                    ),
                )
            }
        }
    }

    @Test
    fun overview_showsTotalsAndRanking_andOpensAPlayer() {
        setContent(previewStatsState)

        composeRule.onNodeWithText("8").assertExists()
        composeRule.onNodeWithText("Jinx").assertExists()
        composeRule.onAllNodesWithText("Ana").onFirst().performClick()
        assertEquals(listOf("name:ana"), players)

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Cleo"))
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
