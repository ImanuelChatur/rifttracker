package com.rifttally.core.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.compose.rememberAsyncImagePainter
import com.rifttally.core.designsystem.component.PreviewRiftThemes
import com.rifttally.core.designsystem.theme.PlayerColor
import com.rifttally.core.designsystem.theme.PlayerColors
import com.rifttally.core.designsystem.theme.RiftTheme

/**
 * A player's circular Legend portrait, optionally with their [score] drawn big on top.
 *
 * Loads [imageUri] (an asset URI from the Legend catalog) with Coil. With no image, no Legend, or a
 * failed load, it shows a circle in [playerColor] with the initial of [legendName] (or of [playerName]
 * when no Legend is picked). Over an image, the score sits on a dark radial scrim with a text shadow,
 * so it stays readable on any portrait; over the fallback it uses [PlayerColor.onColor] (WCAG AA).
 *
 * @param legendName the picked Legend's name, or `null` for none.
 * @param imageUri the Legend's portrait URI, or `null` when there's no image.
 * @param playerName used for the fallback initial and the accessibility label.
 * @param size diameter of the avatar; take it from `RiftTheme.sizes`.
 * @param contentDescription accessibility label; `null` makes the avatar decorative (e.g. when a
 *   visible name label sits next to it).
 */
@Composable
public fun LegendAvatar(
    legendName: String?,
    imageUri: String?,
    playerName: String,
    playerColor: PlayerColor,
    modifier: Modifier = Modifier,
    size: Dp = RiftTheme.sizes.avatarMedium,
    score: Int? = null,
    contentDescription: String? = legendAvatarDescription(legendName, playerName, score),
) {
    val semanticsModifier = if (contentDescription != null) {
        Modifier.clearAndSetSemantics {
            this.contentDescription = contentDescription
            // Announce score changes politely (e.g. after a tap) without stealing focus.
            if (score != null) liveRegion = LiveRegionMode.Polite
        }
    } else {
        Modifier.clearAndSetSemantics {}
    }
    // The painter is always drawn (Coil resolves its size from drawing) but stays empty until loaded.
    // Until it succeeds, and on error, the fallback shows instead.
    val painter = imageUri?.let { rememberAsyncImagePainter(model = it, contentScale = ContentScale.Crop) }
    val painterState = painter?.state?.collectAsState()
    val showImage = painterState?.value is AsyncImagePainter.State.Success
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(if (showImage) Modifier.testTag(LEGEND_ART_TEST_TAG) else Modifier)
            .then(semanticsModifier),
        contentAlignment = Alignment.Center,
    ) {
        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
        if (showImage) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(RiftTheme.sizes.borderThick, playerColor.color, CircleShape),
            )
            if (score != null) ScoreOnPortrait(score = score, size = size)
        } else {
            InitialFallback(
                initial = avatarInitial(legendName, playerName),
                playerColor = playerColor,
                size = size,
                score = score,
            )
        }
    }
}

/** Builds the default accessibility label for [LegendAvatar]. */
@Composable
private fun legendAvatarDescription(legendName: String?, playerName: String, score: Int?): String {
    val base = if (legendName != null) {
        stringResource(R.string.core_ui_legend_avatar_description, playerName, legendName)
    } else {
        stringResource(R.string.core_ui_legend_avatar_description_no_legend, playerName)
    }
    return if (score != null) {
        pluralStringResource(R.plurals.core_ui_legend_avatar_description_with_score, score, base, score)
    } else {
        base
    }
}

@Composable
private fun InitialFallback(initial: String, playerColor: PlayerColor, size: Dp, score: Int?) {
    Box(
        modifier = Modifier
            .size(size)
            .background(playerColor.color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (score == null) {
            Text(
                text = initial,
                style = numberStyle(size, INITIAL_RATIO),
                color = playerColor.onColor,
            )
        } else {
            // The initial stays as a faint watermark so the Legend is still recognizable behind the score.
            Text(
                text = initial,
                style = numberStyle(size, WATERMARK_RATIO),
                color = playerColor.onColor.copy(alpha = WATERMARK_ALPHA),
            )
            Crossfade(targetState = score, label = "score") { value ->
                Text(
                    text = value.toString(),
                    style = numberStyle(size, scoreRatio(value)),
                    color = playerColor.onColor,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.ScoreOnPortrait(score: Int, size: Dp) {
    val colors = RiftTheme.portraitColors
    Box(
        modifier = Modifier
            .matchParentSize()
            // Hold full scrim strength past the digits (~60% of the radius for two digits) so every
            // glyph keeps AA contrast on a bright portrait, then fade toward the ring.
            .background(
                Brush.radialGradient(
                    0f to colors.scrimCenter,
                    SCRIM_SOLID_STOP to colors.scrimCenter,
                    1f to colors.scrimEdge,
                ),
            ),
    )
    val density = LocalDensity.current
    val shadow = with(density) {
        Shadow(
            color = colors.textShadow,
            offset = Offset(x = 0f, y = RiftTheme.spacing.xxs.toPx()),
            blurRadius = RiftTheme.spacing.sm.toPx(),
        )
    }
    // Brief crossfade when the score changes.
    Crossfade(targetState = score, label = "score") { value ->
        Text(
            text = value.toString(),
            style = numberStyle(size, scoreRatio(value)).copy(shadow = shadow),
            color = colors.onScrim,
        )
    }
}

/** A score-style text sized relative to the avatar [size], with line padding trimmed so it centers. */
@Composable
private fun numberStyle(size: Dp, ratio: Float): TextStyle {
    val fontSize = with(LocalDensity.current) { (size * ratio).toSp() }
    return RiftTheme.scoreTypography.scoreLarge.copy(
        fontSize = fontSize,
        lineHeight = fontSize,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both,
        ),
    )
}

private fun scoreRatio(score: Int): Float = if (score in 0..MAX_TWO_DIGITS) SCORE_RATIO else SCORE_RATIO_LONG

// Text sizes as a fraction of the avatar diameter, so the avatar looks the same at every size.
private const val INITIAL_RATIO = 0.48f
private const val WATERMARK_RATIO = 0.82f
private const val WATERMARK_ALPHA = 0.16f
private const val SCORE_RATIO = 0.5f
private const val SCORE_RATIO_LONG = 0.36f
private const val MAX_TWO_DIGITS = 99
private const val SCRIM_SOLID_STOP = 0.7f

// region Previews

/** Stands in for a Legend portrait in previews (a flat, light color: worst case for the score). */
@OptIn(ExperimentalCoilApi::class)
private val previewImageHandler = AsyncImagePreviewHandler { _ -> ColorImage(PlayerColors.Yellow.color.toArgb()) }

private const val PREVIEW_IMAGE_URI = "file:///android_asset/legends/preview.webp"

@Composable
private fun LegendAvatarPreviewRow(score: Int?, size: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(RiftTheme.spacing.md)) {
        LegendAvatar(
            legendName = null,
            imageUri = null,
            playerName = "Ana",
            playerColor = PlayerColors.Orange,
            size = size,
            score = score,
        )
        LegendAvatar(
            legendName = "Jinx",
            imageUri = null,
            playerName = "Ben",
            playerColor = PlayerColors.Sky,
            size = size,
            score = score,
        )
        LegendAvatar(
            legendName = "Viktor",
            imageUri = PREVIEW_IMAGE_URI,
            playerName = "Cleo",
            playerColor = PlayerColors.Green,
            size = size,
            score = score,
        )
    }
}

@PreviewRiftThemes
@Composable
private fun LegendAvatarPreview() {
    RiftTheme {
        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewImageHandler) {
            Surface(color = RiftTheme.colorScheme.background) {
                Column(
                    modifier = Modifier.padding(RiftTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.lg),
                ) {
                    LegendAvatarPreviewRow(score = null, size = RiftTheme.sizes.avatarSmall)
                    LegendAvatarPreviewRow(score = null, size = RiftTheme.sizes.avatarMedium)
                    LegendAvatarPreviewRow(score = 7, size = RiftTheme.sizes.avatarMedium)
                }
            }
        }
    }
}

@PreviewRiftThemes
@Composable
private fun LegendAvatarLargeScorePreview() {
    RiftTheme {
        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewImageHandler) {
            Surface(color = RiftTheme.colorScheme.background) {
                Column(
                    modifier = Modifier.padding(RiftTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(RiftTheme.spacing.lg),
                ) {
                    LegendAvatar(
                        legendName = "Miss Fortune",
                        imageUri = PREVIEW_IMAGE_URI,
                        playerName = "Ana",
                        playerColor = PlayerColors.Pink,
                        size = RiftTheme.sizes.avatarLarge,
                        score = 8,
                    )
                    LegendAvatar(
                        legendName = "Lee Sin",
                        imageUri = null,
                        playerName = "Ben",
                        playerColor = PlayerColors.Blue,
                        size = RiftTheme.sizes.avatarLarge,
                        score = 11,
                    )
                }
            }
        }
    }
}

// endregion
