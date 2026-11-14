package com.rifttally.feature.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.GameId
import com.rifttally.core.ui.LEGEND_ART_TEST_TAG
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Home with and without a game in progress. Records `src/test/screenshots/home_*.png`. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
class HomeScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun activeGame_withArt_dark() =
        capture(activeState(artForBoth = true), darkTheme = true, "home_active_art_dark.png")

    @Test
    fun activeGame_mixedArt_light() =
        capture(activeState(artForBoth = false), darkTheme = false, "home_active_mixed_light.png")

    @Test
    fun noActiveGame_dark() = capture(emptyState(), darkTheme = true, "home_empty_dark.png")

    @Test
    fun noActiveGame_light() = capture(emptyState(), darkTheme = false, "home_empty_light.png")

    @Test
    fun firstRun_noGamesAtAll_dark() = capture(
        HomeUiState.Success(activeGame = null, now = NOW),
        darkTheme = true,
        "home_first_run_dark.png",
    )

    // DESIGN.md §5: text wraps and nothing clips at 200% font scale.
    @Test
    @Config(fontScale = 2f)
    fun activeGame_fontScale200_dark() =
        capture(activeState(artForBoth = true), darkTheme = true, "home_active_font200_dark.png")

    @Test
    @Config(fontScale = 2f)
    fun noActiveGame_fontScale200_light() = capture(emptyState(), darkTheme = false, "home_empty_font200_light.png")

    // DESIGN.md §8: landscape phone (compact height), content capped to a centered column.
    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun activeGame_landscape_dark() =
        capture(activeState(artForBoth = true), darkTheme = true, "home_active_land_dark.png")

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xhdpi")
    fun noActiveGame_landscape_light() = capture(emptyState(), darkTheme = false, "home_empty_land_light.png")

    private fun capture(state: HomeUiState.Success, darkTheme: Boolean, name: String) {
        composeRule.setContent {
            RiftTheme(darkTheme = darkTheme) {
                HomeScreen(
                    uiState = state,
                    onNewGameClick = {},
                    onResumeClick = {},
                    onSettingsClick = {},
                    onHistoryClick = {},
                    onStatsClick = {},
                    onRecentGameClick = {},
                )
            }
        }
        val expected = state.activeGame?.sides.orEmpty().count { it.legendImageUri != null } +
            state.recentGames.sumOf { game -> game.sides.count { it.legendImageUri != null } }
        composeRule.waitUntil(timeoutMillis = ART_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(LEGEND_ART_TEST_TAG, useUnmergedTree = true)
                .fetchSemanticsNodes().size >= expected
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(name)
    }

    private fun activeState(artForBoth: Boolean): HomeUiState.Success = HomeUiState.Success(
        activeGame = ActiveGameUiState(
            gameId = GameId("g3"),
            targetScore = 8,
            sides = persistentListOf(
                ResumeSideUiState("Ana", 6, "orange", "Jinx", TestArt.Warm.uri(256, 256)),
                ResumeSideUiState("Ben", 3, "sky", "Viktor", if (artForBoth) TestArt.Cool.uri(256, 256) else null),
            ),
        ),
        recentGames = recentGames(),
        now = NOW,
    )

    private fun emptyState(): HomeUiState.Success =
        HomeUiState.Success(activeGame = null, recentGames = recentGames(), now = NOW)

    private fun recentGames() = persistentListOf(
        RecentGameUiState(
            GameId("g2"),
            NOW - HOUR,
            persistentListOf(
                ResumeSideUiState("Ana", 8, "orange", "Jinx", TestArt.Warm.uri(128, 128)),
                ResumeSideUiState("Ben", 5, "sky", "Viktor", null),
            ),
            winnerName = "Ana",
        ),
        RecentGameUiState(
            GameId("g1"),
            NOW - DAY,
            persistentListOf(
                ResumeSideUiState("Cleo", 4, "green", null, null),
                ResumeSideUiState("Dan", 8, "pink", "Ahri", TestArt.Bright.uri(128, 128)),
                ResumeSideUiState("Ben", 6, "sky", null, null),
            ),
            winnerName = "Dan",
        ),
        RecentGameUiState(
            GameId("g0"),
            NOW - DAY * 2,
            persistentListOf(
                ResumeSideUiState("Ana & Cleo", 11, "orange", "Jinx", null),
                ResumeSideUiState("Ben & Dan", 7, "sky", null, null),
            ),
            winnerName = null,
        ),
    )

    private companion object {
        const val ART_TIMEOUT_MILLIS = 10_000L
        const val HOUR = 3_600_000L
        const val DAY = HOUR * 24
        const val NOW = DAY * 400
    }
}
