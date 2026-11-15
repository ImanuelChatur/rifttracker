package com.rifttally.core.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.takahirom.roborazzi.captureRoboImage
import com.rifttally.core.model.GameFormat
import java.util.TimeZone
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The shareable result card: 2 players, 4 players, 2v2, with and without art, with and without a winner.
 * Portraits are generated gradients ([TestPortrait]), never real art. Records
 * `src/test/screenshots/result_card_*.png`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
class ResultCardScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val originalTimeZone = TimeZone.getDefault()

    @Before
    fun fixTimeZone() {
        // The card shows a date: keep it the same wherever the test runs.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    private val portraits = persistentMapOf(
        TestPortrait.Warm.uri to TestPortrait.Warm.image(),
        TestPortrait.Cool.uri to TestPortrait.Cool.image(),
        TestPortrait.Bright.uri to TestPortrait.Bright.image(),
    )

    private fun player(name: String, color: String, legend: String? = null, art: TestPortrait? = null) =
        ResultCardPlayer(name, color, legend, art?.uri)

    private fun side(score: Int, winner: Boolean, vararg players: ResultCardPlayer) =
        ResultCardSide(persistentListOf(*players), score, winner)

    @Test
    fun twoPlayers_withArt_andWinner() = capture(
        "result_card_2p_art.png",
        ResultCardState(
            GameFormat.TWO_PLAYERS,
            ENDED_AT,
            persistentListOf(
                side(8, true, player("Ana", "orange", "Jinx", TestPortrait.Warm)),
                side(5, false, player("Ben", "sky", "Viktor", TestPortrait.Bright)),
            ),
        ),
    )

    @Test
    fun fourPlayers_fallbacks_andWinner() = capture(
        "result_card_4p_fallback.png",
        ResultCardState(
            GameFormat.FOUR_PLAYERS,
            ENDED_AT,
            persistentListOf(
                side(3, false, player("Ana", "orange", "Jinx")),
                side(8, true, player("Benedikt the Unbelievably Long-Named", "sky", "Miss Fortune")),
                side(0, false, player("Cleo", "green")),
                side(12, false, player("Player 4", "unknown-color", "Ahri")),
            ),
        ),
        portraits = persistentMapOf(),
    )

    @Test
    fun teams_mixedArt_andWinner() = capture(
        "result_card_2v2_art.png",
        ResultCardState(
            GameFormat.TEAMS_2V2,
            ENDED_AT,
            persistentListOf(
                side(7, false, player("Ana", "orange", "Jinx", TestPortrait.Warm), player("Ben", "sky")),
                side(
                    11,
                    true,
                    player("Cleo", "green", "Viktor", TestPortrait.Cool),
                    player("Dan", "pink", "Ahri", TestPortrait.Bright),
                ),
            ),
        ),
    )

    @Test
    fun twoPlayers_noWinner() = capture(
        "result_card_2p_no_winner.png",
        ResultCardState(
            GameFormat.TWO_PLAYERS,
            ENDED_AT,
            persistentListOf(
                side(4, false, player("Ana", "orange", "Jinx", TestPortrait.Warm)),
                side(4, false, player("Ben", "sky")),
            ),
        ),
    )

    private fun capture(
        name: String,
        state: ResultCardState,
        portraits: ImmutableMap<String, androidx.compose.ui.graphics.ImageBitmap> = this.portraits,
    ) {
        composeRule.setContent {
            ResultCard(state = state, portraits = portraits, modifier = Modifier.testTag(CARD_TAG))
        }
        composeRule.onNodeWithTag(CARD_TAG).captureRoboImage(name)
    }

    private companion object {
        const val CARD_TAG = "result_card"

        /** 2025-11-14 20:00 UTC. */
        const val ENDED_AT = 1_763_150_400_000L
    }
}
