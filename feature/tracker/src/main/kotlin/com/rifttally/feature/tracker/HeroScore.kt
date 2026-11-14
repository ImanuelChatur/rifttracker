package com.rifttally.feature.tracker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.theme.RiftMotionScheme
import com.rifttally.core.designsystem.theme.RiftTheme

/**
 * The panel's score, as large as fits its slot: the font size follows the available height and width
 * (not a fixed sp), so a 2-digit score fills a big panel and still fits a small rotated one. Changes
 * roll vertically (up for +, down for −) on the expressive fast springs, or switch instantly when [reducedMotion].
 *
 * @param horizontalInset space kept clear at each side (for the −/+ glyphs).
 * @param shadow whether to draw a soft drop shadow (over art).
 */
@Composable
internal fun HeroScore(
    points: Int,
    description: String,
    color: Color,
    shadow: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    horizontalInset: Dp = 0.dp,
) {
    val motion = RiftTheme.motion.expressive
    val density = LocalDensity.current
    val textShadow = if (shadow) {
        with(density) {
            Shadow(
                color = RiftTheme.tableColors.artTextShadow,
                offset = Offset(0f, RiftTheme.spacing.xxs.toPx()),
                blurRadius = RiftTheme.spacing.lg.toPx(),
            )
        }
    } else {
        null
    }
    BoxWithConstraints(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = description
            // Announce score changes politely (e.g. after a tap) without stealing focus.
            liveRegion = LiveRegionMode.Polite
        },
        contentAlignment = Alignment.Center,
    ) {
        val width = (maxWidth - horizontalInset * 2).coerceAtLeast(0.dp)
        AnimatedContent(
            targetState = points,
            transitionSpec = {
                if (reducedMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    roll(up = targetState > initialState, motion = motion)
                }
            },
            contentAlignment = Alignment.Center,
            label = "score",
            // Full-size, so the roll and the shadow are clipped at the panel area, not at the glyphs.
            modifier = Modifier.fillMaxSize(),
        ) { value ->
            val fontSize = with(density) { scoreSize(value, maxHeight, width).toSp() }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = value.toString(),
                    color = color,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    style = RiftTheme.scoreTypography.scoreHuge.copy(
                        fontSize = fontSize,
                        lineHeight = fontSize,
                        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                        shadow = textShadow,
                    ),
                    // The line box may be a bit taller than the slot; only empty ascent space overflows.
                    modifier = Modifier.wrapContentSize(unbounded = true),
                )
            }
        }
    }
}

private fun roll(up: Boolean, motion: RiftMotionScheme): ContentTransform {
    val sign = if (up) 1 else -1
    val spatial = motion.fastSpatial<IntOffset>()
    return (slideInVertically(spatial) { height -> sign * height } + fadeIn(motion.fastEffects()))
        .togetherWith(slideOutVertically(spatial) { height -> -sign * height } + fadeOut(motion.fastEffects()))
}

/** Largest size (as a Dp height) at which [value]'s digits fit [height] × [width]. */
private fun scoreSize(value: Int, height: Dp, width: Dp): Dp {
    val digits = value.toString().length
    val byWidth = width / (digits * DIGIT_WIDTH_EM + SIDE_BEARING_EM)
    val byHeight = height * HEIGHT_FILL
    return minOf(byWidth, byHeight).coerceAtLeast(0.dp)
}

/** Barlow Condensed Bold tabular figures are ~0.5em wide; a little slack keeps them clear of clipping. */
private const val DIGIT_WIDTH_EM = 0.54f
private const val SIDE_BEARING_EM = 0.1f

/** Digits are ~0.7em tall, so a font a bit larger than the slot still keeps them inside it. */
private const val HEIGHT_FILL = 1.2f

@PreviewRiftThemes
@Composable
private fun HeroScorePreview() {
    RiftTheme {
        Surface {
            HeroScore(
                points = 18,
                description = "Ana, 18 points",
                color = RiftTheme.colorScheme.onSurface,
                shadow = false,
                reducedMotion = true,
                modifier = Modifier,
            )
        }
    }
}
