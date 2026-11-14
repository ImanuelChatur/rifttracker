package com.rifttally.feature.tracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.ui.rotateLayout

/**
 * Arranges the side panels around the table, each turned toward its seat, with the control [strip]
 * in the middle. Seats follow the setup order, clockwise:
 * - 2 sides (2 players or 2v2 teams): top (180°), bottom (0°).
 * - 3: left (90°), right (270°), bottom (0°).
 * - 4: top (180°), right (270°), bottom (0°), left (90°).
 *
 * [panel] draws side `index`; the [Modifier] it receives already sizes and rotates the slot.
 */
// panelCount is fixed for a game, so only one branch ever composes the [panel] slots.
@Suppress("ktlint:compose:content-slot-reused")
@Composable
internal fun TrackerTableLayout(
    panelCount: Int,
    strip: @Composable () -> Unit,
    panel: @Composable (index: Int, modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gap = RiftTheme.spacing.xs
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
        when (panelCount) {
            3 -> {
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    panel(0, Modifier.weight(1f).seat(LEFT))
                    panel(1, Modifier.weight(1f).seat(RIGHT))
                }
                strip()
                panel(2, Modifier.weight(1f).seat(BOTTOM))
            }

            4 -> {
                panel(0, Modifier.weight(1f).seat(TOP))
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    panel(3, Modifier.weight(1f).seat(LEFT))
                    panel(1, Modifier.weight(1f).seat(RIGHT))
                }
                strip()
                panel(2, Modifier.weight(1f).seat(BOTTOM))
            }

            else -> {
                // Two sides face each other; also the fallback for any other count (one column).
                for (index in 0 until panelCount) {
                    if (index == panelCount / 2) strip()
                    val turns = if (panelCount == 2 && index == 0) TOP else BOTTOM
                    panel(index, Modifier.weight(1f).seat(turns))
                }
                if (panelCount < 2) strip()
            }
        }
    }
}

private const val BOTTOM = 0
private const val LEFT = 1
private const val TOP = 2
private const val RIGHT = 3

/** Fills the slot and turns the panel toward its seat. */
private fun Modifier.seat(quarterTurns: Int): Modifier = fillMaxSize().rotateLayout(quarterTurns)
