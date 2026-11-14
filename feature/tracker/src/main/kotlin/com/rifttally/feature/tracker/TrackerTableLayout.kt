package com.rifttally.feature.tracker

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.ui.rememberReducedMotion
import com.rifttally.core.ui.rotateLayout
import kotlinx.coroutines.delay

/**
 * Arranges the side panels around the table, each turned toward its seat, with the control [strip]
 * floating over the seam in the middle. Seats follow the setup order, clockwise:
 * - 2 sides (2 players or 2v2 teams): top (180°), bottom (0°).
 * - 3: left (90°), right (270°), bottom (0°).
 * - 4: top (180°), right (270°), bottom (0°), left (90°).
 *
 * [panel] draws side `index`; the [Modifier] it receives already sizes, rotates and fades in the slot.
 * `facesStrip` is true for panels whose (own) top edge meets the strip, so they keep it clear.
 * Panels fade in one after another when the table first appears.
 */
// panelCount is fixed for a game, so only one branch ever composes the [panel] slots.
@Suppress("ktlint:compose:content-slot-reused")
@Composable
internal fun TrackerTableLayout(
    panelCount: Int,
    strip: @Composable () -> Unit,
    panel: @Composable (index: Int, modifier: Modifier, facesStrip: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gap = RiftTheme.spacing.xs
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
        when (panelCount) {
            3 -> {
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    panel(0, Modifier.weight(1f).seat(LEFT).then(rememberEnterFade(0)), false)
                    panel(1, Modifier.weight(1f).seat(RIGHT).then(rememberEnterFade(1)), false)
                }
                FloatingStrip(strip)
                panel(2, Modifier.weight(1f).seat(BOTTOM).then(rememberEnterFade(2)), true)
            }

            4 -> {
                panel(0, Modifier.weight(1f).seat(TOP).then(rememberEnterFade(0)), false)
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    panel(3, Modifier.weight(1f).seat(LEFT).then(rememberEnterFade(3)), false)
                    panel(1, Modifier.weight(1f).seat(RIGHT).then(rememberEnterFade(1)), false)
                }
                FloatingStrip(strip)
                panel(2, Modifier.weight(1f).seat(BOTTOM).then(rememberEnterFade(2)), true)
            }

            else -> {
                // Two sides face each other; also the fallback for any other count (one column).
                for (index in 0 until panelCount) {
                    if (index == panelCount / 2) FloatingStrip(strip)
                    val turns = if (panelCount == 2 && index == 0) TOP else BOTTOM
                    val facesStrip = panelCount == 2
                    panel(index, Modifier.weight(1f).seat(turns).then(rememberEnterFade(index)), facesStrip)
                }
                if (panelCount < 2) FloatingStrip(strip)
            }
        }
    }
}

/** Fades a panel in, delayed by its seat [index] so panels appear one after another (clockwise). */
@Composable
private fun rememberEnterFade(index: Int): Modifier {
    val reducedMotion = rememberReducedMotion()
    val motion = RiftTheme.motion
    val alpha = remember { Animatable(if (reducedMotion) 1f else 0f) }
    LaunchedEffect(alpha) {
        delay(index * motion.enterStaggerMillis)
        alpha.animateTo(1f, motion.defaultEffects())
    }
    return Modifier.graphicsLayer { this.alpha = alpha.value }
}

/**
 * A zero-height anchor on the seam between two rows; the strip is centered on it and drawn (and hit
 * tested) above both neighbors, so it floats "between" the players.
 */
@Composable
private fun FloatingStrip(strip: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.dp)
            .zIndex(1f),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.wrapContentHeight(unbounded = true)) { strip() }
    }
}

private const val BOTTOM = 0
private const val LEFT = 1
private const val TOP = 2
private const val RIGHT = 3

/** Fills the slot and turns the panel toward its seat. */
private fun Modifier.seat(quarterTurns: Int): Modifier = fillMaxSize().rotateLayout(quarterTurns)
