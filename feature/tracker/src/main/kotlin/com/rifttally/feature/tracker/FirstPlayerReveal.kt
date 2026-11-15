package com.rifttally.feature.tracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.theme.RiftTheme
import com.rifttally.core.designsystem.theme.reduceMotionEnabled
import kotlinx.coroutines.delay

/** What the reveal shows right now: the highlighted panel and whether the result is out. */
@Stable
internal class FirstPlayerRevealDisplay(highlighted: Int?, settled: Boolean) {
    /** Panel index under the cycling highlight, or `null` before it starts. */
    var highlighted: Int? by mutableStateOf(highlighted)

    /** The highlight has stopped on the chosen panel and "<Name> goes first" shows. */
    var settled: Boolean by mutableStateOf(settled)
}

/**
 * Plays [reveal]: the highlight steps clockwise around the [panelCount] panels for [REVEAL_LAPS] laps,
 * slowing down, then stops on the chosen panel (decided before it starts; purely cosmetic). [onStart]
 * runs as it begins, so if the screen is recreated mid-cycle (rotation) the result shows directly
 * instead of cycling again. After the result has been up for [REVEAL_VISIBLE_MILLIS] (longer if the
 * user's accessibility timeout asks for it) [onDismiss] runs. With reduce motion it shows the result
 * directly.
 */
@Composable
internal fun rememberFirstPlayerReveal(
    reveal: FirstPlayerRevealUiState?,
    panelCount: Int,
    onStart: (pickId: String) -> Unit,
    onDismiss: (pickId: String) -> Unit,
): FirstPlayerRevealDisplay? {
    if (reveal == null) return null
    val skipCycle = reduceMotionEnabled() || !reveal.animate || panelCount < 2
    val display = remember(reveal.pickId) {
        FirstPlayerRevealDisplay(highlighted = if (skipCycle) reveal.panelIndex else null, settled = skipCycle)
    }
    val stepMillis = RiftTheme.motion.revealStepMillis
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val accessibilityManager = LocalAccessibilityManager.current
    LaunchedEffect(reveal.pickId) {
        currentOnStart(reveal.pickId)
        if (!display.settled) {
            val steps = REVEAL_LAPS * panelCount + reveal.panelIndex + 1
            for (step in 0 until steps) {
                display.highlighted = step % panelCount
                // Eases out: each step a little longer than the last, up to [REVEAL_SLOWDOWN]× the first.
                val progress = step.toFloat() / steps
                delay((stepMillis * (1 + (REVEAL_SLOWDOWN - 1) * progress * progress)).toLong())
            }
            display.highlighted = reveal.panelIndex
            display.settled = true
        }
        delay(
            accessibilityManager?.calculateRecommendedTimeoutMillis(
                REVEAL_VISIBLE_MILLIS,
                containsIcons = false,
                containsText = true,
                containsControls = false,
            ) ?: REVEAL_VISIBLE_MILLIS,
        )
        currentOnDismiss(reveal.pickId)
    }
    return display
}

/**
 * Drawn over one panel, inside its seat rotation, so "<Name> goes first" faces that seat. [highlighted]
 * outlines and tints the panel; [showResult] shows the result card.
 */
@Composable
internal fun FirstPlayerRevealOverlay(
    name: String,
    highlighted: Boolean,
    showResult: Boolean,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = reduceMotionEnabled()
    val motion = RiftTheme.motion.expressive
    val highlight by animateFloatAsState(
        targetValue = if (highlighted) 1f else 0f,
        animationSpec = if (reducedMotion) RiftTheme.motion.none() else motion.fastEffects(),
        label = "revealHighlight",
    )
    val shape = RiftTheme.shapes.extraLarge
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = highlight }
                .clip(shape)
                .background(RiftTheme.colorScheme.onSurface.copy(alpha = HIGHLIGHT_TINT_ALPHA))
                .border(RiftTheme.sizes.edgeGlow * HIGHLIGHT_BORDER_FACTOR, RiftTheme.colorScheme.onSurface, shape),
        )
        AnimatedVisibility(
            visible = showResult,
            enter = if (reducedMotion) {
                EnterTransition.None
            } else {
                scaleIn(motion.fastSpatial()) + fadeIn(motion.fastEffects())
            },
            exit = if (reducedMotion) ExitTransition.None else fadeOut(motion.defaultEffects()),
        ) {
            FirstPlayerCard(name = name, modifier = Modifier.padding(RiftTheme.spacing.lg))
        }
    }
}

/** "<Name> goes first", announced to screen readers when it appears. */
@Composable
private fun FirstPlayerCard(name: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        shape = RiftTheme.shapes.extraLarge,
        color = RiftTheme.colorScheme.primaryContainer,
        contentColor = RiftTheme.colorScheme.onPrimaryContainer,
        shadowElevation = RiftTheme.elevation.level3,
    ) {
        Text(
            text = stringResource(R.string.feature_tracker_goes_first, name),
            style = RiftTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = RiftTheme.spacing.xl, vertical = RiftTheme.spacing.lg),
        )
    }
}

/**
 * Covers the whole table while the reveal shows: a tap anywhere dismisses it. For screen readers it is
 * one button reading "<Name> goes first" (double-tap to dismiss).
 */
@Composable
internal fun FirstPlayerRevealTapCatcher(name: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.feature_tracker_goes_first, name)
    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = stringResource(R.string.feature_tracker_goes_first_dismiss),
                role = Role.Button,
                onClick = onDismiss,
            )
            .semantics { contentDescription = description },
    )
}

/** Full laps the highlight makes before stopping. */
private const val REVEAL_LAPS = 2

/** The last step lasts this many times the first one. */
private const val REVEAL_SLOWDOWN = 4f

/** How long the result stays up before dismissing itself. */
private const val REVEAL_VISIBLE_MILLIS = 3_500L

private const val HIGHLIGHT_TINT_ALPHA = 0.16f
private const val HIGHLIGHT_BORDER_FACTOR = 2

@PreviewRiftThemes
@Composable
private fun FirstPlayerCardPreview() {
    RiftTheme {
        FirstPlayerCard(name = "Ana")
    }
}
