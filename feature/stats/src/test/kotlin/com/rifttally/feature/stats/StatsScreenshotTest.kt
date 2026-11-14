package com.rifttally.feature.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.ui.LEGEND_ART_TEST_TAG
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Stats overview, Legends tab, empty state and player detail. Records `src/test/screenshots/stats_*.png`. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = PHONE_TALL)
class StatsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun overview_dark() = captureStats(overview(), darkTheme = true, name = "stats_overview_dark.png")

    @Test
    fun overview_light() = captureStats(overview(), darkTheme = false, name = "stats_overview_light.png")

    @Test
    fun legends_dark() =
        captureStats(overview().copy(selectedTab = StatsTab.LEGENDS), darkTheme = true, name = "stats_legends_dark.png")

    @Test
    @Config(qualifiers = PHONE)
    fun empty_light() = capture(darkTheme = false, name = "stats_empty_light.png", artCount = 0) {
        StatsScreen(uiState = StatsUiState.Empty, actions = StatsActions())
    }

    @Test
    fun player_dark() = capturePlayer(darkTheme = true, name = "stats_player_dark.png")

    @Test
    fun player_light() = capturePlayer(darkTheme = false, name = "stats_player_light.png")

    private val jinx = LegendRowUiState("jinx", "Jinx", TestArt.Warm.uri(128, 128), games = 6, winRatePercent = 67)

    private fun overview() = previewStatsState.copy(
        mostPlayedLegend = jinx,
        legends = persistentListOf(
            jinx,
            LegendRowUiState("viktor", "Viktor", null, games = 4, winRatePercent = 25),
            LegendRowUiState("ahri", "Ahri", TestArt.Cool.uri(128, 128), games = 2, winRatePercent = 50),
        ),
    )

    private fun captureStats(state: StatsUiState.Success, darkTheme: Boolean, name: String) {
        val art = if (state.selectedTab == StatsTab.LEGENDS) {
            state.legends.count { it.imageUri != null }
        } else {
            1
        }
        capture(darkTheme, name, art) { StatsScreen(uiState = state, actions = StatsActions()) }
    }

    private fun capturePlayer(darkTheme: Boolean, name: String) {
        val state = previewPlayerState.copy(favoriteLegend = jinx)
        capture(darkTheme, name, artCount = 1) {
            PlayerStatsScreen(uiState = state, onBackClick = {}, onPlayerClick = {})
        }
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

    private companion object {
        const val ART_TIMEOUT_MILLIS = 10_000L
    }
}

private const val PHONE = "w411dp-h891dp-xhdpi"
private const val PHONE_TALL = "w411dp-h1100dp-xhdpi"
