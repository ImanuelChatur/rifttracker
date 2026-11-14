package com.rifttally.feature.history

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
import kotlin.test.assertEquals
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Semantics tests of the stateless history screen (JVM, Robolectric). */
@RunWith(RobolectricTestRunner::class)
class HistoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = RuntimeEnvironment.getApplication()

    private val clicked = mutableListOf<GameId>()
    private val deleted = mutableListOf<GameId>()
    private val formats = mutableListOf<GameFormat?>()
    private var undos = 0
    private var backs = 0

    private fun setContent(state: HistoryUiState) {
        composeRule.setContent {
            RiftTheme {
                HistoryScreen(
                    uiState = state,
                    actions = HistoryActions(
                        onBackClick = { backs++ },
                        onGameClick = { clicked += it },
                        onFormatSelect = { formats += it },
                        onDeleteGame = { deleted += it },
                        onUndoDelete = { undos++ },
                    ),
                )
            }
        }
    }

    @Test
    fun rows_showNamesScoresAndWinner_andOpenTheGame() {
        setContent(HistoryUiState.Success(games = persistentListOf(previewGame), hasAnyGames = true))

        composeRule.onNodeWithText("Ana").assertExists()
        composeRule.onNodeWithText("Jinx").assertExists()
        composeRule.onNodeWithText("8").assertExists()
        composeRule.onNodeWithText(context.getString(R.string.feature_history_winner, "Ana")).performClick()

        assertEquals(listOf(GameId("g1")), clicked)
    }

    @Test
    fun menu_deletesTheGame() {
        setContent(HistoryUiState.Success(games = persistentListOf(previewGame), hasAnyGames = true))

        composeRule.onNodeWithContentDescription(context.getString(R.string.feature_history_row_menu, "Ana, Ben"))
            .performClick()
        composeRule.onNodeWithText(context.getString(R.string.feature_history_delete)).performClick()

        assertEquals(listOf(GameId("g1")), deleted)
    }

    @Test
    fun filterChips_selectFormat() {
        setContent(HistoryUiState.Success(games = persistentListOf(previewGame), hasAnyGames = true))

        composeRule.onNodeWithText(context.getString(R.string.feature_history_filter_all)).assertIsSelected()
        composeRule.onNodeWithText(
            context.getString(R.string.feature_history_format_teams),
        ).performScrollTo().performClick()

        assertEquals(listOf<GameFormat?>(GameFormat.TEAMS_2V2), formats)
    }

    @Test
    fun emptyHistory_andEmptyFilter_showMessages() {
        setContent(HistoryUiState.Success(hasAnyGames = false))
        composeRule.onNodeWithText(context.getString(R.string.feature_history_empty)).assertExists()
    }

    @Test
    fun emptyFilterResult_showsFilterMessage() {
        setContent(HistoryUiState.Success(hasAnyGames = true, selectedFormat = GameFormat.THREE_PLAYERS))
        composeRule.onNodeWithText(context.getString(R.string.feature_history_empty_filter)).assertExists()
    }

    @Test
    fun deletedGame_showsUndoSnackbar() {
        setContent(HistoryUiState.Success(hasAnyGames = true, recentlyDeleted = GameId("g1")))

        composeRule.onNodeWithText(context.getString(R.string.feature_history_deleted)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.feature_history_undo)).performClick()
        composeRule.waitForIdle()

        assertEquals(1, undos)
    }

    @Test
    fun back_invokesCallback() {
        setContent(HistoryUiState.Loading)

        composeRule.onNodeWithContentDescription(context.getString(R.string.feature_history_back)).performClick()

        assertEquals(1, backs)
    }
}
