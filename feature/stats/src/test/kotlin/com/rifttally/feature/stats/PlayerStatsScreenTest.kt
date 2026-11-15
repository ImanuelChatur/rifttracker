package com.rifttally.feature.stats

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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

/** Semantics tests of the stateless player stats screen (JVM, Robolectric). */
@RunWith(RobolectricTestRunner::class)
class PlayerStatsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun showsStats_andHeadToHeadOpensTheOpponent() {
        val opened = mutableListOf<String>()
        composeRule.setContent {
            RiftTheme {
                PlayerStatsScreen(uiState = previewPlayerState, onBackClick = {}, onPlayerClick = { opened += it })
            }
        }

        composeRule.onNodeWithText("Ana").assertExists()
        composeRule.onNodeWithText(context.getString(R.string.feature_stats_percent, 71)).assertExists()
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Jinx"))

        val games = context.resources.getQuantityString(R.plurals.feature_stats_games, 5, 5)
        val ben = context.getString(R.string.feature_stats_head_to_head_description, "Ana", 4, "Ben", 1, games)
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription(ben))
        composeRule.onNodeWithContentDescription(ben).performClick()

        assertEquals(listOf("name:ben"), opened)
    }

    @Test
    fun notFound_showsMessage() {
        composeRule.setContent {
            RiftTheme {
                PlayerStatsScreen(uiState = PlayerStatsUiState.NotFound(), onBackClick = {}, onPlayerClick = {})
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.feature_stats_not_found)).assertExists()
    }

    @Test
    fun notFoundInAPeriod_saysSo() {
        composeRule.setContent {
            RiftTheme {
                PlayerStatsScreen(
                    uiState = PlayerStatsUiState.NotFound(StatsPeriod.THIS_YEAR),
                    onBackClick = {},
                    onPlayerClick = {},
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.feature_stats_period_empty)).assertExists()
    }

    @Test
    fun firstPlayerTiles_showBothRates() {
        composeRule.setContent {
            RiftTheme { PlayerStatsScreen(uiState = previewPlayerState, onBackClick = {}, onPlayerClick = {}) }
        }

        composeRule.onNode(hasScrollToIndexAction())
            .performScrollToNode(hasText(context.getString(R.string.feature_stats_not_first)))
        composeRule.onNodeWithText(context.getString(R.string.feature_stats_going_first)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.feature_stats_not_first)).assertExists()
        composeRule.onNodeWithText("67%").assertExists()
        composeRule.onNodeWithText("75%").assertExists()
    }
}
