package com.rifttally.feature.tracker

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.model.CounterId
import com.rifttally.core.model.PlayerId
import com.rifttally.core.model.SideId
import com.rifttally.core.ui.LEGEND_ART_TEST_TAG
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Table screenshots: every format, with and without Legend art, light and dark, plus the target-reached
 * state. Records `src/test/screenshots/tracker_<case>.png` with `recordRoborazziDebug`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
open class TrackerScreenScreenshotTest(private val case: Case) {

    data class Case(
        val format: Format,
        val withArt: Boolean,
        val darkTheme: Boolean,
        val atTarget: Boolean = false,
        val landscape: Boolean = false,
    ) {
        val fileName: String
            get() = buildString {
                append("tracker_").append(format.name.lowercase())
                append(if (withArt) "_art" else "_noart")
                if (atTarget) append("_target")
                if (landscape) append("_land")
                append(if (darkTheme) "_dark" else "_light")
                append(".png")
            }

        override fun toString(): String = fileName
    }

    enum class Format { TwoPlayers, ThreePlayers, FourPlayers, Teams }

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun capture() {
        val state = trackerState(case)
        composeRule.setContent {
            RiftTheme(darkTheme = case.darkTheme) {
                TrackerScreen(
                    uiState = state,
                    onBackClick = {},
                    onPointsChange = { _, _ -> },
                    onUndo = {},
                    onTargetScoreChange = {},
                    onEndGame = {},
                    onKeepPlaying = {},
                    onRestart = {},
                    onCounterAdd = { _, _ -> },
                    onCounterChange = { _, _, _ -> },
                    onCounterRemove = { _, _ -> },
                )
            }
        }
        if (case.withArt) {
            val expected = state.panels.sumOf { panel -> panel.players.count { it.legendWideImageUri != null } }
            composeRule.waitUntil(timeoutMillis = ART_TIMEOUT_MILLIS) {
                composeRule.onAllNodesWithTag(LEGEND_ART_TEST_TAG, useUnmergedTree = true)
                    .fetchSemanticsNodes().size >= expected
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(case.fileName)
    }

    companion object {
        private const val ART_TIMEOUT_MILLIS = 10_000L

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Case> = Format.entries.flatMap { format ->
            listOf(true, false).flatMap { art -> listOf(true, false).map { dark -> Case(format, art, dark) } }
        } + listOf(
            Case(Format.TwoPlayers, withArt = true, darkTheme = true, atTarget = true),
            Case(Format.TwoPlayers, withArt = false, darkTheme = false, atTarget = true),
        )

        // Ana gets the brightest art: the worst case for score contrast (DESIGN.md §3).
        private val arts = listOf(TestArt.Bright, TestArt.Cool, TestArt.Warm, TestArt.Warm)

        private val roster = listOf(
            Triple("Ana", "orange", "Jinx"),
            Triple("Ben", "sky", "Viktor"),
            Triple("Cleo", "green", null),
            Triple("Dan", "pink", "Ahri"),
        )

        private fun player(index: Int, withArt: Boolean, xp: Int?): PanelPlayerUiState {
            val (name, color, legend) = roster[index]
            val art = if (withArt && legend != null) arts[index] else null
            return PanelPlayerUiState(
                playerId = PlayerId("p$index"),
                name = name,
                colorId = color,
                legendName = legend,
                legendImageUri = art?.uri(width = 256, height = 256),
                legendWideImageUri = art?.uri(),
                counters = xp?.let {
                    persistentListOf(CounterUiState(CounterId("xp"), "XP", it))
                } ?: persistentListOf(),
            )
        }

        private fun trackerState(case: Case): TrackerUiState.Success {
            val art = case.withArt
            val panels = when (case.format) {
                Format.TwoPlayers -> listOf(
                    side(0, points = if (case.atTarget) 8 else 6, target = case.atTarget, player(0, art, xp = 3)),
                    side(1, points = 4, target = false, player(1, art, xp = null)),
                )

                Format.ThreePlayers -> (0 until 3).map {
                    side(it, points = it * 3 + 1, false, player(it, art, xp = it))
                }

                Format.FourPlayers -> (0 until 4).map {
                    side(it, points = it * 2 + 3, false, player(it, art, xp = it + 1))
                }

                Format.Teams -> listOf(
                    side(0, points = 7, target = false, player(0, art, xp = 2), player(1, art, xp = null)),
                    side(1, points = 11, target = false, player(2, art, xp = null), player(3, art, xp = 4)),
                )
            }.toImmutableList()
            return previewTrackerState.copy(
                panels = panels,
                targetScore = 8,
                winnerBanner = if (case.atTarget) WinnerBannerUiState(persistentListOf("Ana")) else null,
            )
        }

        private fun side(index: Int, points: Int, target: Boolean, vararg players: PanelPlayerUiState) =
            SidePanelUiState(
                sideId = SideId("s$index"),
                points = points,
                isAtTarget = target,
                players = players.toList().toImmutableList(),
            )
    }
}
