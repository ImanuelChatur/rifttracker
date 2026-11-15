package com.rifttally.feature.tracker

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.designsystem.theme.reduceMotionEnabled
import com.rifttally.core.ui.rotateLayout
import kotlinx.coroutines.delay

/**
 * Arranges the side panels around the table, each turned toward its seat, with the control [strip]
 * floating over the seam in the middle. Seats follow the setup order, clockwise:
 * - 2 sides (2 players or 2v2 teams): top (180°), bottom (0°).
 * - 3: left (90°), right (270°), bottom (0°).
 * - 4: top (180°), right (270°), bottom (0°), left (90°).
 *
 * In landscape (compact height, DESIGN.md §8) the seats on the short edges get the full height instead,
 * so no score is squeezed into a short strip:
 * - 2: left (90°) | right (270°), strip over the vertical seam.
 * - 3: left (90°) | bottom (0°) | right (270°), strip at the top of the middle panel.
 * - 4: left (90°) | top (180°) over bottom (0°) | right (270°), strip on the middle seam.
 *
 * In portrait with 3–4 players the strip hangs from the seam into the bottom seat (never over a sideways
 * seat); otherwise it is centered on the seam.
 *
 * [panel] draws side `index`; the [Modifier] it receives already sizes, rotates and fades in the slot.
 * `stripInset` is how much of the panel's own top edge the strip covers (0 when it doesn't meet it).
 * `quarterTurns` is the seat's clockwise rotation, so popups for that seat can face the same way.
 * Panels fade in one after another when the table first appears.
 */
@Composable
internal fun TrackerTableLayout(
    panelCount: Int,
    strip: @Composable () -> Unit,
    panel: @Composable (index: Int, modifier: Modifier, stripInset: Dp, quarterTurns: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth > maxHeight) {
            LandscapeTable(panelCount = panelCount, strip = strip, panel = panel)
        } else {
            PortraitTable(panelCount = panelCount, height = maxHeight, strip = strip, panel = panel)
        }
    }
}

// panelCount is fixed for a game, so only one branch ever composes the [panel] slots.
@Suppress("ktlint:compose:content-slot-reused")
@Composable
private fun LandscapeTable(
    panelCount: Int,
    strip: @Composable () -> Unit,
    panel: @Composable (index: Int, modifier: Modifier, stripInset: Dp, quarterTurns: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gap = RiftTheme.spacing.xs
    val halfBar = RiftTheme.sizes.controlBarHeight / 2
    Box(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            when (panelCount) {
                3 -> {
                    panel(0, Modifier.weight(1f).seat(LEFT).then(rememberEnterFade(0)), 0.dp, LEFT)
                    panel(2, Modifier.weight(1f).seat(BOTTOM).then(rememberEnterFade(2)), halfBar * 2, BOTTOM)
                    panel(1, Modifier.weight(1f).seat(RIGHT).then(rememberEnterFade(1)), 0.dp, RIGHT)
                }

                4 -> {
                    panel(3, Modifier.weight(1f).seat(LEFT).then(rememberEnterFade(3)), 0.dp, LEFT)
                    Column(
                        modifier = Modifier.weight(LANDSCAPE_MIDDLE_WEIGHT),
                        verticalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        panel(0, Modifier.weight(1f).seat(TOP).then(rememberEnterFade(0)), halfBar, TOP)
                        FloatingStrip(strip)
                        panel(2, Modifier.weight(1f).seat(BOTTOM).then(rememberEnterFade(2)), halfBar, BOTTOM)
                    }
                    panel(1, Modifier.weight(1f).seat(RIGHT).then(rememberEnterFade(1)), 0.dp, RIGHT)
                }

                else -> for (index in 0 until panelCount) {
                    // The horizontal bar crosses the vertical seam: it covers about half its width of each
                    // panel's own top edge.
                    val turns = if (index == 0) LEFT else RIGHT
                    val inset = if (panelCount == 2) RiftTheme.sizes.minTouchTarget * 2 else 0.dp
                    panel(index, Modifier.weight(1f).seat(turns).then(rememberEnterFade(index)), inset, turns)
                }
            }
        }
        when (panelCount) {
            4 -> Unit

            // Over the middle panel's far edge.
            3 -> Box(modifier = Modifier.align(Alignment.TopCenter).zIndex(1f)) { strip() }

            else -> Box(modifier = Modifier.align(Alignment.Center).zIndex(1f)) { strip() }
        }
    }
}

// panelCount is fixed for a game, so only one branch ever composes the [panel] slots.
@Suppress("ktlint:compose:content-slot-reused")
@Composable
private fun PortraitTable(
    panelCount: Int,
    height: Dp,
    strip: @Composable () -> Unit,
    panel: @Composable (index: Int, modifier: Modifier, stripInset: Dp, quarterTurns: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gap = RiftTheme.spacing.xs
    val halfBar = RiftTheme.sizes.controlBarHeight / 2
    // 3–4 players: the bar sits wholly inside the bottom panel (which keeps that band free), so it never
    // covers a sideways seat's edge, glyph or controls (DESIGN.md §8 containment).
    val fullBar = RiftTheme.sizes.controlBarHeight
    // ...and is that much taller, so its score gets the same room as the other seats'.
    val rows = if (panelCount == 4) 3 else 2
    val rowHeight = (height - fullBar - gap * rows) / rows
    val bottomWeight = if (rowHeight > 0.dp) (rowHeight + fullBar) / rowHeight else 1f
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
        when (panelCount) {
            3 -> {
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    panel(0, Modifier.weight(1f).seat(LEFT).then(rememberEnterFade(0)), 0.dp, LEFT)
                    panel(1, Modifier.weight(1f).seat(RIGHT).then(rememberEnterFade(1)), 0.dp, RIGHT)
                }
                FloatingStrip(strip, insideBelow = true)
                panel(2, Modifier.weight(bottomWeight).seat(BOTTOM).then(rememberEnterFade(2)), fullBar, BOTTOM)
            }

            4 -> {
                panel(0, Modifier.weight(1f).seat(TOP).then(rememberEnterFade(0)), 0.dp, TOP)
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    panel(3, Modifier.weight(1f).seat(LEFT).then(rememberEnterFade(3)), 0.dp, LEFT)
                    panel(1, Modifier.weight(1f).seat(RIGHT).then(rememberEnterFade(1)), 0.dp, RIGHT)
                }
                FloatingStrip(strip, insideBelow = true)
                panel(2, Modifier.weight(bottomWeight).seat(BOTTOM).then(rememberEnterFade(2)), fullBar, BOTTOM)
            }

            else -> {
                // Two sides face each other; also the fallback for any other count (one column).
                for (index in 0 until panelCount) {
                    if (index == panelCount / 2) FloatingStrip(strip)
                    val turns = if (panelCount == 2 && index == 0) TOP else BOTTOM
                    val inset = if (panelCount == 2) halfBar else 0.dp
                    panel(index, Modifier.weight(1f).seat(turns).then(rememberEnterFade(index)), inset, turns)
                }
                if (panelCount < 2) FloatingStrip(strip)
            }
        }
    }
}

/** Fades a panel in, delayed by its seat [index] so panels appear one after another (clockwise). */
@Composable
private fun rememberEnterFade(index: Int): Modifier {
    val reducedMotion = reduceMotionEnabled()
    val motion = RiftTheme.motion
    val alpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(alpha) {
        delay(index * motion.enterStaggerMillis)
        alpha.animateTo(1f, motion.expressive.defaultEffects())
    }
    return Modifier.graphicsLayer { this.alpha = alpha.value }
}

/**
 * A zero-height anchor on the seam between two rows; the strip is drawn (and hit tested) above both
 * neighbors, so it floats "between" the players. Centered on the seam, or with [insideBelow] hanging
 * from it into the panel below only (that panel reserves the band via its strip inset).
 */
@Composable
private fun FloatingStrip(strip: @Composable () -> Unit, modifier: Modifier = Modifier, insideBelow: Boolean = false) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.dp)
            .zIndex(1f),
        contentAlignment = if (insideBelow) Alignment.TopCenter else Alignment.Center,
    ) {
        Box(
            modifier = Modifier.wrapContentHeight(
                align = if (insideBelow) Alignment.Top else Alignment.CenterVertically,
                unbounded = true,
            ),
        ) { strip() }
    }
}

private const val BOTTOM = 0
private const val LEFT = 1
private const val TOP = 2
private const val RIGHT = 3

/** The middle column (top and bottom seats) is a bit wider than the side seats in landscape. */
private const val LANDSCAPE_MIDDLE_WEIGHT = 1.4f

/** Fills the slot and turns the panel toward its seat. */
private fun Modifier.seat(quarterTurns: Int): Modifier = fillMaxSize().rotateLayout(quarterTurns)
