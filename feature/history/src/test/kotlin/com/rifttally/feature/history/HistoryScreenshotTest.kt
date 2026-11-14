package com.rifttally.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.GameFormat
import com.rifttally.core.model.GameId
import com.rifttally.core.ui.LEGEND_ART_TEST_TAG
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** History list, empty states and game detail. Records `src/test/screenshots/history_*.png`. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = PHONE)
class HistoryScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun list_dark() = captureList(darkTheme = true, name = "history_list_dark.png")

    @Test
    fun list_light() = captureList(darkTheme = false, name = "history_list_light.png")

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun list_landscape_dark() = captureList(darkTheme = true, name = "history_list_landscape_dark.png")

    @Test
    @Config(qualifiers = PHONE, fontScale = 2f)
    fun list_fontScale200_dark() = captureList(darkTheme = true, name = "history_list_font200_dark.png")

    @Test
    fun empty_dark() = capture(darkTheme = true, name = "history_empty_dark.png", artCount = 0) {
        HistoryScreen(uiState = HistoryUiState.Success(hasAnyGames = false), actions = HistoryActions())
    }

    @Test
    fun emptyFilter_light() = capture(darkTheme = false, name = "history_empty_filter_light.png", artCount = 0) {
        HistoryScreen(
            uiState = HistoryUiState.Success(hasAnyGames = true, selectedFormat = GameFormat.THREE_PLAYERS),
            actions = HistoryActions(),
        )
    }

    @Test
    @Config(qualifiers = PHONE_TALL)
    fun detail_dark() = captureDetail(darkTheme = true, name = "history_detail_dark.png")

    @Test
    @Config(qualifiers = PHONE_TALL)
    fun detail_light() = captureDetail(darkTheme = false, name = "history_detail_light.png")

    private fun captureList(darkTheme: Boolean, name: String) {
        val state = listState()
        val art = state.games.sumOf { game -> game.sides.count { it.legendImageUri != null } }
        capture(darkTheme, name, art) { HistoryScreen(uiState = state, actions = HistoryActions()) }
    }

    private fun captureDetail(darkTheme: Boolean, name: String) {
        val state = previewDetailState.copy(
            sides = persistentListOf(
                HistorySideUiState("Ana", "Jinx", "orange", "Jinx", TestArt.Warm.uri(128, 128), 8, isWinner = true),
                HistorySideUiState("Ben", "Viktor", "sky", "Viktor", null, 5, isWinner = false),
            ),
        )
        capture(darkTheme, name, artCount = 1) { GameDetailScreen(uiState = state, onBackClick = {}) }
    }

    private fun capture(darkTheme: Boolean, name: String, artCount: Int, content: @Composable () -> Unit) {
        composeRule.setContent { RiftTheme(darkTheme = darkTheme) { content() } }
        composeRule.waitUntil(timeoutMillis = ART_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(LEGEND_ART_TEST_TAG, useUnmergedTree = true)
                .fetchSemanticsNodes().size >= artCount
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(name)
    }

    private fun listState() = HistoryUiState.Success(
        games = persistentListOf(
            HistoryGameUiState(
                gameId = GameId("g3"),
                endedAt = ENDED_AT,
                format = GameFormat.TWO_PLAYERS,
                sides = persistentListOf(
                    HistorySideUiState("Ana", "Jinx", "orange", "Jinx", TestArt.Warm.uri(128, 128), 8, true),
                    HistorySideUiState("Ben", "Viktor", "sky", "Viktor", null, 5, false),
                ),
                winnerName = "Ana",
            ),
            HistoryGameUiState(
                gameId = GameId("g2"),
                endedAt = ENDED_AT - DAY,
                format = GameFormat.THREE_PLAYERS,
                sides = persistentListOf(
                    HistorySideUiState("Cleo", null, "green", null, null, 4, false),
                    HistorySideUiState("Dan", "Ahri", "pink", "Ahri", TestArt.Bright.uri(128, 128), 8, true),
                    HistorySideUiState("Ben", null, "sky", null, null, 6, false),
                ),
                winnerName = "Dan",
            ),
            HistoryGameUiState(
                gameId = GameId("g1"),
                endedAt = ENDED_AT - 2 * DAY,
                format = GameFormat.TEAMS_2V2,
                sides = persistentListOf(
                    HistorySideUiState("Ana & Cleo", "Jinx & Viktor", "orange", "Jinx", null, 9, false),
                    HistorySideUiState("Ben & Dan", null, "sky", null, null, 9, false),
                ),
                winnerName = null,
            ),
        ),
        hasAnyGames = true,
    )

    private companion object {
        const val ART_TIMEOUT_MILLIS = 10_000L
        const val DAY = 86_400_000L
        const val ENDED_AT = 1_763_150_400_000L
    }
}

private const val PHONE = "w411dp-h891dp-xhdpi"
private const val PHONE_TALL = "w411dp-h1300dp-xhdpi"
private const val LANDSCAPE = "w891dp-h411dp-xhdpi"
